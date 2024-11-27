package immutable;

/** Like java.util.function.Function, but able to throw a specific kind of exception. */
@FunctionalInterface
public interface ThrowingFunction<Arg, Result, Err extends Throwable> {
    Result apply(Arg arg) throws Err;
}
