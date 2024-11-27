package immutable;

/** Represents `first` prepended to the sequence `rest`. */
public record Cons<X>(X first, List<X> rest) implements List<X> {
    /* TEMPLATE
    public RESULT templateMethod() {
        ... first ... rest.templateMethod() ...;
    }
    */

    /**
     * Notice we are playing a little trick here! We return Cons<Y> instead of List<Y>
     * as specified in the interface. We are *refining the type* of the result, because
     * we know that in this case, the result will always be non-Nil. */
    @Override public <Y, E extends Throwable> Cons<Y> map(ThrowingFunction<X, Y, E> f) throws E {
        return new Cons<>(f.apply(first), rest.map(f));
    }
}
