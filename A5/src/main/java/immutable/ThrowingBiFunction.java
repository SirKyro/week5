package immutable;

/** Like java.util.function.BiFunction, but able to throw a specific kind of exception. */
@FunctionalInterface
public interface ThrowingBiFunction<Arg1, Arg2, Result, Err extends Throwable> {
    Result apply(Arg1 arg1, Arg2 arg2) throws Err;
}
