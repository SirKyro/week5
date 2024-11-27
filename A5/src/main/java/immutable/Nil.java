package immutable;

/** Represents the empty sequence. */
public record Nil<X>() implements List<X> {
    /* TEMPLATE
    public RESULT templateMethod() {
        return ...;
    }
    */

    /**
     * Notice we are playing a little trick here! We return Nil<Y> instead of List<Y>
     * as specified in the interface. We are *refining the type* of the result, because
     * we know that in this case, the result will always be Nil. */
    @Override public <Y, E extends Throwable> Nil<Y> map(ThrowingFunction<X, Y, E> f) throws E {
        return new Nil<>();
    }
}
