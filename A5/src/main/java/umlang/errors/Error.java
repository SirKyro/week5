package umlang.errors;

/** Describes an interpretation error - see Exp.interp(...) */
public sealed interface Error permits
    UnboundVariable,
    BadArgumentCount,
    MethodNotFound,
    ExpectedNum,
    ExpectedBool,
    ExpectedCell,
    ExpectedFn,
    ExpectedObj,
    UserException,
    UninitializedGlobal
{
    /** Throws `this` as an InterpretationException. */
    default <Y> Y signal() throws InterpretationException {
        throw new InterpretationException(this);
    }

    /** Retrieve a human-readable description of this error. */
    default String errorString() {
        return switch (this) {
            case UnboundVariable(var name) -> "Unbound variable: " + name;
            case BadArgumentCount(var expected, var actual) -> "Expected " + expected + " arguments, got " + actual;
            case MethodNotFound(var selector) -> "Method not found: " + selector;
            case ExpectedNum(var actual) -> "Expected number: " + actual;
            case ExpectedBool(var actual) -> "Expected boolean: " + actual;
            case ExpectedCell(var actual) -> "Expected cell: " + actual;
            case ExpectedFn(var actual) -> "Expected function: " + actual;
            case ExpectedObj(var actual) -> "Expected object: " + actual;
            case UserException(var exn) -> "Exception thrown: " + exn;
            case UninitializedGlobal(var name) -> "Uninitialized global variable: " + name;
        };
    }
}
