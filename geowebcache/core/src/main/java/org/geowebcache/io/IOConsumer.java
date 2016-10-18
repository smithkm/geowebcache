package org.geowebcache.io;

import java.io.IOException;
import java.util.Objects;

/**
 * Equivalent to java.util.function.Consumer but may throw IOException.
 *
 * @param <T>
 */
@FunctionalInterface
public interface IOConsumer<T> {
    
    void accept(T t) throws IOException;
    
    default IOConsumer<T> andThen(IOConsumer<? super T> after) throws IOException {
        Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
    }
}