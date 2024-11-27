package immutable.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import immutable.None;
import immutable.Some;

public class TestMaybe {
    @Test void test_valueOr() {
        assertEquals(new Some<>(123).valueOr(234).intValue(), 123);
        assertEquals(new None<Integer>().valueOr(234).intValue(), 234);
        assertEquals(new Some<>(123).valueOr(() -> 234).intValue(), 123);
        assertEquals(new None<Integer>().valueOr(() -> 234).intValue(), 234);
    }

    @Test void test_orElse() {
        assertEquals(new Some<>(123), new Some<>(123).orElse(() -> new Some<>(234)));
        assertEquals(new Some<>(234), new None<Integer>().orElse(() -> new Some<>(234)));
        assertEquals(new Some<>(123), new Some<>(123).orElse(() -> new None<>()));
        assertEquals(new None<>(), new None<Integer>().orElse(() -> new None<>()));
    }

    @Test void test_unwrap() {
        assertEquals(new Some<>(123).unwrap().intValue(), 123);
        assertThrows(RuntimeException.class, () -> new None<Integer>().unwrap());
    }

    @Test void test_map() {
        assertEquals(new None<Integer>().map(Object::toString), new None<String>());
        assertEquals(new Some<>(123).map(Object::toString), new Some<>("123"));
    }

    @Test void test_andThen() {
        assertEquals(new None<Integer>().andThen((v) -> new Some<>(v.toString())), new None<String>());
        assertEquals(new Some<>(123).andThen((v) -> new Some<>(v.toString())), new Some<>("123"));
        assertEquals(new Some<>(123).andThen((v) -> new None<>()), new None<>());
    }

    @Test void test_is_methods() {
        assertTrue(new None<Integer>().isNone());
        assertFalse(new None<Integer>().isSome());
        assertTrue(new Some<>(123).isSome());
        assertFalse(new Some<>(123).isNone());
    }
}
