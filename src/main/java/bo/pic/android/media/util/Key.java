package bo.pic.android.media.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class is intended to be used to provide type-safe programming with generic types and java key-value containers.
 * <p/>
 * Example: {@link java.util.Map} defines a separate type variable for values but we might want to keep values of different types
 * at the same map object. This class allows to write type-safe code for that:
 * <pre>
 *     // Define target keys as public constants at a shared place.
 *     Key&lt;Integer&gt; ID_KEY = new Key&lt;&gt;("ID", Integer.class);
 *     Key&lt;String&gt; MESSAGE_KEY = new Key&lt;&gt;("MESSAGE", String.class);
 *     ...
 *     // Create a container at data producer.
 *     Map&lt;K, V&gt; context = new HashMap&lt;&gt;();
 *     ID_KEY.{@link #put(java.util.Map, Object) put}(context, 42);
 *     MESSAGE_KEY.{@link #put(java.util.Map, Object) put}(context, "hi there");
 *     ...
 *     // Consume the data
 *     void process(Map&lt;?, ?&gt; context) {
 *         String message = MESSAGE_KEY.{@link #get(java.util.Map) get}(context);
 *         if (message != null) {
 *             showMessage(message);
 *         }
 *     }
 * </pre>
 */
public class Key<T> {

    @Nonnull public static final Map<Key<?>, ?> NO_DATA = Collections.emptyMap();

    @Nonnull private final String   id;
    @Nonnull private final Class<?> dataType;

    public Key(@Nonnull String id, @Nonnull Class<?> dataType) {
        this.id = id;
        this.dataType = dataType;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public Class<?> getDataType() {
        // This method is necessary at least for easing custom (de)serialization.
        return dataType;
    }

    /**
     * Allows to perform type-safe casting of the given object to the target key type.
     *
     * @param object object to cast
     * @return given object casted to the target static type
     * @throws IllegalArgumentException     if given object can not be cast to the key's type
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public T cast(@Nonnull Object object) {
        if (!dataType.isInstance(object)) {
            throw new IllegalArgumentException(String.format("Can't cast object of type %s to %s for the key %s",
                    object.getClass().getName(), dataType.getName(), id));
        }
        return (T) object;
    }

    @SuppressWarnings({ "UnnecessaryLocalVariable", "unchecked" })
    @Nullable
    public T put(@Nonnull Map<Key<?>, ?> dataHolder, @Nonnull T data) {
        Map raw = dataHolder;
        Object result = raw.put(this, data);
        return (T) result;
    }

    /**
     * Convenient way to create new data holder with the given key stored there for the current key.
     * <p/>
     * I.e. this method works as below:
     * <pre>
     *     Map&lt;<Key&lt;?&gt;, ?&gt; map = ... // Create a map.
     *     Key&lt;?&gt; key = ...; // Current key object.
     *     key.put(map, data);
     *     return map;
     * </pre>
     *
     * @param data    data to store for the current key
     * @return        data holder with the given data stored for the current key in it
     */
    @Nonnull
    public Map<Key<?>, ?> put(@Nonnull T data) {
        Map<Key<?>, ?> result = new HashMap<Key<?>, Object>();
        put(result, data);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public T remove(@Nonnull Map<Key<?>, ?> dataHolder) {
        return (T) dataHolder.remove(this);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public T get(@Nonnull Map<?, ?> dataHolder) {
        Object result = dataHolder.get(this);
        if (result == null) {
            return null;
        }
        if (!dataType.isAssignableFrom(result.getClass())) {
            throw new IllegalArgumentException(String.format(
                    "Error on retreiving data for key '%s': expected a value which IS-A %s but it's not (%s)",
                    this, dataType, result.getClass()
            ));
        }
        return (T) result;
    }

    @Nonnull
    public T get(@Nonnull Map<?, ?> dataHolder, @Nonnull T defaultValue) {
        T result = get(dataHolder);
        return result == null ? defaultValue : result;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Key that = (Key) o;
        return id.equals(that.id);
    }

    @Override
    public String toString() {
        return id;
    }
}
