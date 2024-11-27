package immutable;

/** Like java.util.function.Predicate, but able to throw a specific kind of exception. */
@FunctionalInterface
public interface ThrowingPredicate<Arg, Err extends Throwable> {
    boolean test(Arg arg) throws Err;
}
