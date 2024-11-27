package immutable;

/** Like ThrowingBiFunction, but for three arguments */
@FunctionalInterface
public interface ThrowingTriFunction<A, B, C, R, Err extends Throwable> {
    R apply(A a, B b, C c) throws Err;
}
