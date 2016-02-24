package org.geowebcache.util;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public enum Optionals {
    ;

    /**
     * Try each supplier in order and return the first result which is present, or an empty Optional
     * if none are present.
     * @param suppliers
     * @return
     */
    @SafeVarargs
    public static <T> Optional<T> firstPresent(Supplier<Optional<T>>... suppliers) {
        return firstPresent(Stream.of(suppliers));
    }
    
    
    /**
     * Try each supplier in order and return the first result which is present, or an empty Optional
     * if none are present.
     * @param suppliers
     * @return
     */
    public static <T> Optional<T> firstPresent(Stream<Supplier<Optional<T>>> suppliers) {
        return suppliers.sequential()
                .map(Supplier::get)
                .filter(Optional::isPresent)
                .findFirst()
                .orElseGet(Optional::empty);
    }
    
    /**
     * Try each supplier in order and return the first result which is present, or an empty Optional
     * if none are present.
     * @param suppliers
     * @return
     */
    public static <T> Optional<T> firstPresent(Collection<Supplier<Optional<T>>> suppliers) {
       return firstPresent(suppliers.stream());
       
    }
    
    /**
     * Try each supplier a result which is present, or an empty Optional if none are present.
     * @param suppliers
     * @return
     */
    public static <T> Optional<T> anyPresent(Stream<Supplier<Optional<T>>> suppliers) {
        return suppliers
                .map(Supplier::get)
                .filter(Optional::isPresent)
                .findAny()
                .orElseGet(Optional::empty);
    }
    
    /**
     * Returns a 1 element stream containing the value of the Optional, or an empty stream if the
     * Optional is empty.  Optional should gain a way to do this itself in Java 9.
     * @param opt
     * @return
     */
    public static <T> Stream<T> stream(Optional<T> opt) {
        return opt.map(Stream::of).orElseGet(Stream::empty);
    }
}
