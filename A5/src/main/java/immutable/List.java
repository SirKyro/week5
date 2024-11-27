package immutable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An immutable.List<X> is a sequence of zero or more Xs.
 */
public sealed interface List<X> extends Iterable<X> permits Nil, Cons {
    /** Convenient way of writing `(List<X>) new Nil<X>()` without the cast. */
    static <X> List<X> empty() {
        return new Nil<>();
    }

    /** Return `this` with `other` appended. */
    default List<X> append(List<X> other) {
        return switch (this) {
            case Nil() -> other;
            case Cons(var first, var rest) -> new Cons<>(first, rest.append(other));
        };
    }

    /** Append items in `this` to `xs`. SIDE EFFECT: mutates `xs`. */
    default void appendTo(java.util.List<X> xs) {
        switch (this) {
            case Nil() -> {}
            case Cons(var first, var rest) -> {
                xs.add(first);
                rest.appendTo(xs);
            }
        }
    }

    /** Natural fold for Lists. */
    default <Y, E extends Throwable> Y fold(Y ifNil, ThrowingBiFunction<X, Y, Y, E> ifCons) throws E {
        return switch (this) {
            case Nil() -> ifNil;
            case Cons(var first, var rest) -> ifCons.apply(first, rest.fold(ifNil, ifCons));
        };
    }

    /**
     * Natural fold over two Lists simultaneously.
     * PRECONDITION: lengths of the two lists are the same.
     */
    static <X, Y, Result, E extends Throwable> Result fold(
            Result ifNil,
            ThrowingTriFunction<X, Y, Result, Result, E> ifCons,
            List<X> xs,
            List<Y> ys
    ) throws E {
        return switch (xs) {
            case Nil() -> ifNil;
            case Cons(var firstX, var restX) -> switch (ys) {
                case Nil() -> throw new RuntimeException("Internal error: ragged lists in fold");
                case Cons(var firstY, var restY) -> ifCons.apply(firstX, firstY, fold(ifNil, ifCons, restX, restY));
            };
        };
    }

    /** Return the length of `xs`. */
    default int length() {
        return this.fold(0, (_x, count) -> count + 1);
    }

    /** Map `f` over `this`. */
    <Y, E extends Throwable> List<Y> map(ThrowingFunction<X, Y, E> f) throws E;

    /** Map `f` over `xs` and `ys`. PRECONDITION: xs.length() == ys.length() */
    static <X, Y, Z, E extends Throwable> List<Z> map(ThrowingBiFunction<X, Y, Z, E> f, List<X> xs, List<Y> ys) throws E {
        // Implemented with a while() loop to avoid easily-avoidable stack overflow errors.
        List<Z> accRev = new Nil<>();
        while (xs instanceof Cons(var firstX, var restX)) {
            switch (ys) {
                case Nil() ->
                    // Will not happen, by precondition.
                    throw new IllegalArgumentException("xs and ys were not the same length");
                case Cons(var firstY, var restY) -> {
                    accRev = new Cons<>(f.apply(firstX, firstY), accRev);
                    xs = restX;
                    ys = restY;
                }
            }
        }
        return accRev.reverse();
    }

    /** Retrieve the first sublist of `this` where `first` matches `p`, if any. */
    default <E extends Throwable> Maybe<X> find(ThrowingPredicate<X, E> p) throws E {
        return switch (this) {
            case Nil() -> new None<>();
            case Cons(var first, var rest) -> p.test(first) ? new Some<>(first) : rest.find(p);
        };
    }

    /** True iff every element of `this` is true wrt `p` */
    default <E extends Throwable> boolean every(ThrowingPredicate<X, E> p) throws E {
        return switch (this) {
            case Nil() -> true;
            case Cons(var first, var rest) -> p.test(first) && rest.every(p);
        };
    }

    /** Return `this`, but reversed. */
    default List<X> reverse() {
        // Implemented using a while loop to avoid easily-avoidable stack overflow errors.
        // Notice the strong similarity to the tail-recursive helper function reverseHelper, commented out below!
        List<X> reversed = new Nil<>();
        List<X> remaining = this;
        while (remaining instanceof Cons(var first, var rest)) {
            reversed = new Cons<>(first, reversed);
            remaining = rest;
        }
        return reversed;
    }

    // -- COMMENTED OUT NATURAL-RECURSIVE VERSION OF reverse().
    // The currently-in-use implementation uses a while loop because Java doesn't handle
    // recursion properly. Note the strong similarity of the structure of the code
    // between reverse as implemented and reverseHelper as shown here.
    //
    // // Only ever called from xs.reverse().
    // // INVARIANT: xs === append(reverse(reversed), remaining)
    // // INVARIANT: reverse(xs) === append(reverse(remaining), reversed)
    // private static <X> List<X> reverseHelper(List<X> remaining, List<X> reversed) {
    //     return switch (remaining) {
    //         case Nil() -> reversed;
    //         case Cons(var first, var rest) -> reverseHelper(rest, new Cons<>(first, reversed));
    //     };
    // }
    //
    // /** Return `this`, but reversed. */
    // default List<X> reverse() {
    //     return List.reverseHelper(this, new Nil<>());
    // }
    //
    // PS. Normally I never leave commented-out blocks like this in code that's saved
    // in a Git repository, because I can always use Git to view the code when I want
    // to. It's here for pedagogical reasons :-)

    /** Construct a List from multiple arguments */
    @SafeVarargs
    static <X> List<X> of(X... xs) {
        List<X> v = new Nil<>();
        for (int i = xs.length - 1; i >= 0; i--) {
            v = new Cons<>(xs[i], v);
        }
        return v;
    }

    /** Retrieve element at index `i`. Throws IndexOutOfBoundsException if `i` is negative or >= the length of `this`. */
    default X get(int i) {
        if (i < 0) throw new IndexOutOfBoundsException();
        var xs = this;
        while (true) {
            switch (xs) {
                case Nil() -> throw new IndexOutOfBoundsException();
                case Cons(var first, var rest) -> {
                    if (i == 0) {
                        return first;
                    } else {
                        i--;
                        xs = rest;
                    }
                }
            }
        }
    }

    /** Implement Java iteration protocol. */
    @Override default Iterator<X> iterator() {
        return new Iterator<>() {
            private List<X> cursor = List.this;

            @Override
            public boolean hasNext() {
                return !(cursor instanceof Nil);
            }

            @Override
            public X next() {
                return switch (cursor) {
                    case Nil() -> throw new NoSuchElementException();
                    case Cons(var first, var rest) -> {
                        cursor = rest;
                        yield first;
                    }
                };
            }
        };
    }

    /** Answer None unless `this` is a Nil */
    default Maybe<Nil<X>> nil() {
        return switch (this) {
            case Nil<X> n -> new Some<>(n);
            case Cons<X> ignored -> new None<>();
        };
    }

    /** Answer None unless `this` is a Cons */
    default Maybe<Cons<X>> cons() {
        return switch (this) {
            case Nil<X> ignored -> new None<>();
            case Cons<X> c -> new Some<>(c);
        };
    }

    /* --------------------------------------------------------------------------- */
    /* Useful methods for our tests - convert between this and java.util.List */

    /** Convert from a java.util.List */
    static <X> List<X> fromList(java.util.List<X> items) {
        List<X> v = new Nil<>();
        for (var x : items.reversed()) {
            v = new Cons<>(x, v);
        }
        return v;
    }

    /** Convert to a java.util.List */
    static <X> java.util.List<X> toList(List<X> xs) {
        var items = new ArrayList<X>();
        while (true) {
            switch (xs) {
                case Nil() -> {
                    return items;
                }
                case Cons(var first, var rest) -> {
                    items.add(first);
                    xs = rest;
                }
            }
        }
    }
}
