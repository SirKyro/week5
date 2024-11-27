package immutable;

/** Represents an optional X. */
public sealed interface Maybe<X> permits None, Some {
    /** Extract value from a Some, else answer alternative */
    default X valueOr(X alternative) {
        return switch (this) {
            case None() -> alternative;
            case Some(var value) -> value;
        };
    }

    /** Monadic bind, also known as "flatMap". Apply `ifSome` if Some, else None. Compare with map(). */
    default <Y, E extends Throwable> Maybe<Y> andThen(ThrowingFunction<X, Maybe<Y>, E> ifSome) throws E {
        return switch (this) {
            case None() -> new None<>();
            case Some(var value) -> ifSome.apply(value);
        };
    }

    /** Alternation. Apply `ifNone` if None, else answer `this`. Compare with valueOr(). */
    default <E extends Throwable> Maybe<X> orElse(ThrowingSupplier<Maybe<? extends X>, E> ifNone) throws E {
        return switch (this) {
            case None() -> ifNone.get().map((v) -> v); // the map() is there because the compiler otherwise can't prove the necessary subtyping relation
            case Some<X> s -> s;
        };
    }

    /** Extract value from a Some, else answer ifNone.get(). Compare with orElse(). */
    default <E extends Throwable> X valueOr(ThrowingSupplier<? extends X, E> ifNone) throws E {
        return switch (this) {
            case None() -> ifNone.get();
            case Some(var value) -> value;
        };
    }

    /** Extract value from a Some, else throw a RuntimeException */
    default X unwrap() {
        return valueOr(() -> {
            throw new RuntimeException("Maybe<X> value not available");
        });
    }

    /** Transform value in a Some; leave None unchanged. Compare with andThen(). */
    default <Y, E extends Throwable> Maybe<Y> map(ThrowingFunction<X, Y, E> f) throws E {
        return switch (this) {
            case None() -> new None<>();
            case Some(var value) -> new Some<>(f.apply(value));
        };
    }

    /** True iff this is None */
    default boolean isNone() {
        return this instanceof None<X>;
    }

    /** True iff this is Some */
    default boolean isSome() {
        return this instanceof Some<X>;
    }
}