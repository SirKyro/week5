package immutable;

/** Like java.util.function.Consumer, but able to throw a specific kind of exception. */
@FunctionalInterface
public interface ThrowingConsumer<Value, Err extends Throwable> {
    void accept(Value value) throws Err;
}
