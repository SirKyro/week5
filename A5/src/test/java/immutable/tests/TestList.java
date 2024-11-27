package immutable.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import immutable.Cons;
import immutable.List;
import immutable.Nil;
import immutable.None;
import immutable.Some;

import java.util.ArrayList;
import java.util.Arrays;

public class TestList {
    @Test void test_convert_forward() {
        assertEquals(
            List.toList(new Cons<>("a", new Cons<>("b", new Nil<>()))),
            Arrays.asList("a", "b"));
    }

    @Test void test_convert_back() {
        assertEquals(
            List.fromList(Arrays.asList("a", "b")),
            new Cons<>("a", new Cons<>("b", new Nil<>())));
    }

    @Test void test_length() {
        assertEquals(new Nil<Integer>().length(), 0);
        assertEquals(List.of(1, 2, 3).length(), 3);
    }

    @Test void test_reverse_method() {
        assertEquals(new Nil<String>().reverse(), new Nil<String>());
        assertEquals(
            List.of("a", "b", "c", "d").reverse(),
            List.of("d", "c", "b", "a"));
    }

    @Test void test_append_method() {
        var empty = new Nil<String>();
        var ab = new Cons<>("a", new Cons<>("b", empty));
        var cd = new Cons<>("c", new Cons<>("d", empty));
        assertEquals(empty.append(empty), empty);
        assertEquals(ab.append(empty), ab);
        assertEquals(empty.append(cd), cd);
        assertEquals(ab.append(cd), List.of("a", "b", "c", "d"));
    }

    @Test void test_appendTo() {
        var xs = new ArrayList<String>();
        var empty = new Nil<String>();
        var ab = new Cons<>("a", new Cons<>("b", empty));
        var cd = new Cons<>("c", new Cons<>("d", empty));
        ab.appendTo(xs);
        ab.appendTo(xs); // yes, twice
        cd.appendTo(xs);
        assertEquals(xs, Arrays.asList("a", "b", "a", "b", "c", "d"));
    }

    @Test void test_fold_method() {
        var xs = List.of(1, 2, 3, 4, 5);
        assertEquals(xs.fold(0, (x, a) -> a + x).intValue(), 15);
    }

    @Test void test_map() {
        assertEquals(
            List.of(1, 2, 3, 4, 5).map((v) -> v + 1),
            List.of(2, 3, 4, 5, 6));
    }

    @Test void test_map2() {
        assertEquals(List.of(), List.map((a, b) -> a + " " + b, List.of(), List.of()));
        assertEquals(
            List.of("1 a" , "2 b", "3 c"),
            List.map((a, b) -> a + " " + b, List.of(1, 2, 3), List.of("a", "b", "c")));
    }

    @Test void test_find() {
        assertEquals(
            List.of("abc", "def", "ghi").find((s) -> s.charAt(0) == 'd'),
            new Some<>("def"));
        assertEquals(
            List.of("abc", "def", "ghi").find((s) -> s.charAt(0) == 'z'),
            new None<String>());
    }

    @Test void test_fold2() {
        assertEquals(
            List.fold(
                "",
                (a, b, acc) -> a + ":" + b + " " + acc,
                List.of(1, 2, 3),
                List.of("a", "b", "c")),
            "1:a 2:b 3:c ");
    }

    @Test void test_get() {
        assertEquals(List.of(0, 1, 2).get(1).intValue(), 1);
        assertThrows(IndexOutOfBoundsException.class, () -> List.of(0, 1, 2).get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> List.of(0, 1, 2).get(11));
    }

    @Test void test_iteration() {
        var sum = 0;
        for (var each : List.of(1, 2, 3, 4, 5)) {
            sum += each;
        }
        assertEquals(sum, 15);
    }

    @Test void test_every() {
        assertTrue(List.of(1, 2, 3, 4).every((i) -> i > 0));
        assertFalse(List.of(1, -2, 3, -4).every((i) -> i > 0));
    }

    @Test void test_to_maybe() {
        assertEquals(List.of().nil(), new Some<>(new Nil<Integer>()));
        assertEquals(List.of(1).nil(), new None<Nil<Integer>>());
        assertEquals(List.of().cons(), new None<Cons<Integer>>());
        assertEquals(List.of(1).cons(), new Some<>(new Cons<>(1, new Nil<>())));
    }
}
