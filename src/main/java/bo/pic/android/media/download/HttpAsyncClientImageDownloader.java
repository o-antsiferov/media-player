package bo.pic.android.media.download;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.util.IoUtil;
import bo.pic.android.media.util.Logger;
import bo.pic.android.media.util.ProcessingCallback;
import bo.pic.repackaged.org.apache.http.HttpEntity;
import bo.pic.repackaged.org.apache.http.HttpResponse;
import bo.pic.repackaged.org.apache.http.HttpStatus;
import bo.pic.repackaged.org.apache.http.client.methods.HttpGet;
import bo.pic.repackaged.org.apache.http.concurrent.FutureCallback;
import bo.pic.repackaged.org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.StatusLine;

public class HttpAsyncClientImageDownloader implements ImageDownloader {

    @Nonnull private static final Future<?> NO_OP = new Future<Object>() {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return true;
        }

        @Override
        public boolean isCancelled() {
            return true;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Nullable
        @Override
        public Object get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Nullable
        @Override
        public Object get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    };

    @Nonnull private final CloseableHttpAsyncClient mClient;

    public HttpAsyncClientImageDownloader(@Nonnull CloseableHttpAsyncClient client) {
        mClient = client;
    }

    @Nonnull
    @Override
    public Future<?> download(@Nonnull final String imageUri, @Nonnull final ProcessingCallback<byte[]> callback) {
        if (!mClient.isRunning()) {
            callback.onFail(null);
            return NO_OP;
        }
        return mClient.execute(new HttpGet(imageUri), new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                int statusCode = result.getStatusLine().getStatusCode();
                if(statusCode == HttpStatus.SC_OK) {
                    HttpEntity entity = result.getEntity();
                    try {
                        callback.onSuccess(IoUtil.toByteArray(entity.getContent()));
                    } catch (IOException exception) {
                        Logger.w(HttpAsyncClientImageDownloader.class, "Unexpected I/O exception occurred on attempt "
                                                                       + "to assemble data downloaded from %s as a byte array", exception, imageUri);
                        callback.onFail(exception);
                    }
                } else {
                    Logger.w(HttpAsyncClientImageDownloader.class, "Unexpected status code received %s from %s", statusCode, imageUri);
                }
            }

            @Override
            public void failed(Exception ex) {
                callback.onFail(ex);
            }

            @Override
            public void cancelled() {
                // Do nothing
                Logger.d(HttpAsyncClientImageDownloader.class, "Cancelled a request do download data from %s", imageUri);
            }
        });
    }
}
