package immutable;

/** Like java.util.function.Supplier, but able to throw a specific kind of exception. */
@FunctionalInterface
public interface ThrowingSupplier<Result, Err extends Throwable> {
    Result get() throws Err;
}
