package bo.pic.android.media.cache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.util.FileUtil;
import bo.pic.android.media.util.Function;
import bo.pic.android.media.util.IoUtil;
import bo.pic.android.media.util.Logger;

public class BaseDiskCache<K> implements DiskCache<K> {

    @Nonnull private Map<String/* file name */, Long> mCacheContents = new LinkedHashMap<String, Long>(16, .75f, true);

    @Nonnull private final File mCacheDirectory;
    @Nonnull private final Function<K, String> mFileNameFactory;

    private final long mMaxSizeInBytes;

    private long mCacheSize;

    public BaseDiskCache(@Nonnull File cacheDirectory, @Nonnull Function<K, String> fileNameFactory, long maxSizeInBytes) {
        mCacheDirectory = cacheDirectory;
        mFileNameFactory = fileNameFactory;
        mMaxSizeInBytes = maxSizeInBytes;
        refresh();
    }

    public synchronized void refresh() {
        // Create root cache dir if necessary.
        if (!mCacheDirectory.exists()) {
            if (!mCacheDirectory.mkdirs()) {
                String message = String.format("Unable to create cache directory [%s]", mCacheDirectory.getAbsolutePath());
                Logger.e(BaseDiskCache.class, message);
                throw new IllegalStateException(message);
            }
        }

        mCacheSize = 0;
        mCacheContents.clear();

        // Fill files info in order to use it later during cache eviction.
        File[] children = mCacheDirectory.listFiles();
        if (children == null) {
            return;
        }
        Map<Long, List<File>> tmp = new TreeMap<>();
        for (File child : children) {
            if (!child.isFile()) {
                continue;
            }
            long key = child.lastModified();
            List<File> files = tmp.get(key);
            if (files == null) {
                tmp.put(key, files = new ArrayList<>());
            }
            files.add(child);
        }
        for (Map.Entry<Long, List<File>> entry : tmp.entrySet()) {
            for (File file : entry.getValue()) {
                mCacheContents.put(file.getName(), file.length());
                mCacheSize += file.length();
            }
        }

        long bytesToClear = mCacheSize - mMaxSizeInBytes;
        if (bytesToClear > 0) {
            trimCache(bytesToClear);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public synchronized byte[] put(@Nonnull K key, @Nonnull byte[] value) {
        final String fileName = mFileNameFactory.apply(key);
        Long oldSize = mCacheContents.remove(fileName);
        long bytesToEvict = mCacheSize + value.length - (oldSize == null ? 0 : oldSize) - mMaxSizeInBytes;
        if (bytesToEvict > 0) {
            trimCache(bytesToEvict);
        }
        File tempFile = new File(mCacheDirectory, fileName + ".tmp");
        try {
            FileUtil.write(value, tempFile);
            FileUtil.move(tempFile, new File(mCacheDirectory, fileName));
            mCacheContents.put(fileName, (long) value.length);
            mCacheSize += value.length - (oldSize == null ? 0 : oldSize);
        } catch (IOException e) {
            Logger.e(BaseDiskCache.class, "Unable to create temp file [%s] for key [%s]", tempFile.getName(), key);
            tempFile.delete();
        }
        return null;
    }

    @Nullable
    @Override
    public synchronized byte[] get(@Nonnull K key) {
        final String fileName = mFileNameFactory.apply(key);
        mCacheContents.get(fileName); // Update 'least recently used' info.
        File file = new File(mCacheDirectory, fileName);
        if (!file.exists()) {
            return null;
        }

        try {
            return IoUtil.toByteArray(file);
        } catch (IOException e) {
            Logger.e(BaseDiskCache.class, "Unable to read from file [%s] for key [%s]", fileName, key);
            remove(key);
        }
        return null;
    }

    @Nonnull
    @Override
    public File getFile(K key) {
        return new File(mCacheDirectory, mFileNameFactory.apply(key));
    }

    @Override
    public synchronized byte[] remove(@Nonnull K key) {
        final String fileName = mFileNameFactory.apply(key);
        mCacheContents.remove(fileName);
        File file = new File(mCacheDirectory, fileName);
        if (file.isFile()) {
            long size = file.length();
            boolean removed = file.delete();
            if (removed) {
                mCacheSize -= size;
            } else {
                Logger.d(BaseDiskCache.class, "Unable to delete cache entry for filename [%s], key [%s]", fileName, key);
            }
        }
        return null;
    }

    @Override
    public synchronized void clear() {
        // Delete all cache files.
        File[] files = mCacheDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                boolean deleted = file.delete();
                if (!deleted) {
                    Logger.e(BaseDiskCache.class, "Unable to delete file [%s]", file.getAbsolutePath());
                }
            }
        }
        mCacheContents.clear();
        mCacheSize = 0;
    }

    private void trimCache(final long bytesToEvict) {
        long evicted = 0;
        for (Iterator<Map.Entry<String, Long>> it = mCacheContents.entrySet().iterator(); evicted < bytesToEvict && it.hasNext();) {
            Map.Entry<String, Long> entry = it.next();
            File file = new File(mCacheDirectory, entry.getKey());
            if (file.isFile()) {
                boolean removed = file.delete();
                if (removed) {
                    it.remove();
                    mCacheContents.remove(entry.getKey());
                    mCacheSize -= entry.getValue();
                    evicted += entry.getValue();
                } else {
                    Logger.w(BaseDiskCache.class, "Unable to delete cached file [%s]", file.getAbsolutePath());
                }
            } else {
                it.remove();
            }
        }
    }
}