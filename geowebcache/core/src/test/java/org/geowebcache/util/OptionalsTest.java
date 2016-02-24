package org.geowebcache.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.lang.String;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import co.unruly.matchers.OptionalMatchers;
import co.unruly.matchers.StreamMatchers;

public class OptionalsTest {
    
    @Test
    public void testFirstPresentEmpty() {
        Stream<Supplier<Optional<String>>> s = Stream.empty();
        
        Optional<String> result = Optionals.firstPresent(s);
        
        assertThat(result, OptionalMatchers.empty());
    }
    
    @Test
    public void testFirstPresentOneEmpty() {
        Stream<Supplier<Optional<String>>> s = Stream.<Supplier<Optional<String>>>
            of(Optional::empty);
        
        Optional<String> result = Optionals.firstPresent(s);
        
        assertThat(result, OptionalMatchers.empty());
    }
    
    @Test
    public void testFirstPresentTwoEmpty() {
        Stream<Supplier<Optional<String>>> s = Stream.<Supplier<Optional<String>>>
            of(Optional::empty, Optional::empty);
        
        Optional<String> result = Optionals.firstPresent(s);
        
        assertThat(result, OptionalMatchers.empty());
    }
    
    @Test
    public void testFirstPresentOneValue() {
        Stream<Supplier<Optional<String>>> s = Stream.<Supplier<Optional<String>>>
            of(()->Optional.of("Test"));
        
        Optional<String> result = Optionals.firstPresent(s);
        
        assertThat(result, OptionalMatchers.contains("Test"));
    }
    
    @Test
    public void testFirstPresentTakeFirst() {
        Stream<Supplier<Optional<String>>> s = Stream.<Supplier<Optional<String>>>
            of(()->Optional.of("Test1"), ()->Optional.of("Test2"));
        
        Optional<String> result = Optionals.firstPresent(s);
        
        assertThat(result, OptionalMatchers.contains("Test1"));
    }
    
    @Test
    public void testFirstPresentSkipNotPresent() {
        Stream<Supplier<Optional<String>>> s = Stream.<Supplier<Optional<String>>>
            of(Optional::empty, ()->Optional.of("Test2"));
        
        Optional<String> result = Optionals.firstPresent(s);
        
        assertThat(result, OptionalMatchers.contains("Test2"));
    }
    
    @Test
    public void testFirstPresentShortCircuit() {
        Stream<Supplier<Optional<String>>> s = Stream.<Supplier<Optional<String>>>
            of(()->Optional.of("Test1"), ()->{Assert.fail("This supplier should not have been called");return null;});
        
        Optional<String> result = Optionals.firstPresent(s);
        
        assertThat(result, OptionalMatchers.contains("Test1"));
    }
    
    @Test
    public void testAnyPresentEmpty() {
        Stream<Supplier<Optional<String>>> s = Stream.empty();
        
        Optional<String> result = Optionals.anyPresent(s);
        
        assertThat(result, OptionalMatchers.empty());
    }
    
    @Test
    public void testAnyPresentOneEmpty() {
        Stream<Supplier<Optional<String>>> s = Stream.<Supplier<Optional<String>>>
            of(Optional::empty);
        
        Optional<String> result = Optionals.anyPresent(s);
        
        assertThat(result, OptionalMatchers.empty());
    }
    
    @Test
    public void testAnyPresentTwoEmpty() {
        Stream<Supplier<Optional<String>>> s = Stream.<Supplier<Optional<String>>>
            of(Optional::empty, Optional::empty);
        
        Optional<String> result = Optionals.anyPresent(s);
        
        assertThat(result, OptionalMatchers.empty());
    }
    
    @Test
    public void testAnyPresentOneValue() {
        Stream<Supplier<Optional<String>>> s = Stream.<Supplier<Optional<String>>>
            of(()->Optional.of("Test"));
        
        Optional<String> result = Optionals.anyPresent(s);
        
        assertThat(result, OptionalMatchers.contains("Test"));
    }
    
    @Test
    public void testAnyPresentTakeEither() {
        Stream<Supplier<Optional<String>>> s = Stream.<Supplier<Optional<String>>>
            of(()->Optional.of("Test1"), ()->Optional.of("Test2"));
        
        Optional<String> result = Optionals.firstPresent(s);
        
        assertThat(result, OptionalMatchers.contains(Matchers.either(Matchers.is("Test1")).or(Matchers.is("Test2"))));
    }
    
    @Test
    public void testAnyPresentSkipNotPresent() {
        Stream<Supplier<Optional<String>>> s = Stream.<Supplier<Optional<String>>>
            of(Optional::empty, ()->Optional.of("Test2"));
        
        Optional<String> result = Optionals.anyPresent(s);
        
        assertThat(result, OptionalMatchers.contains("Test2"));
    }
    
    @Test
    public void testAnyPresentShortCircuit() {
        AtomicBoolean getTried = new AtomicBoolean(false);
        
        Supplier<Optional<String>> sup1 = ()->{
            assertThat(getTried.getAndSet(true), is(false));
            return Optional.of("Test1");
        };
        Supplier<Optional<String>> sup2 = ()->{
            assertThat(getTried.getAndSet(true), is(false));
            return Optional.of("Test2");
        };
        Stream<Supplier<Optional<String>>> s = Stream.<Supplier<Optional<String>>>
            of(sup1, sup2);
        
        Optional<String> result = Optionals.firstPresent(s);
        
        assertThat(result, OptionalMatchers.contains(Matchers.either(Matchers.is("Test1")).or(Matchers.is("Test2"))));
    }
    
    @Test
    public void testStreamPresent() {
        Optional<String> o = Optional.of("Test");
        Stream<String> result = Optionals.stream(o);
        assertThat(result, StreamMatchers.contains("Test"));
    }
    
    @Test
    public void testStreamEmpty() {
        Optional<String> o = Optional.empty();
        Stream<String> result = Optionals.stream(o);
        assertThat(result, StreamMatchers.contains());
    }
}
