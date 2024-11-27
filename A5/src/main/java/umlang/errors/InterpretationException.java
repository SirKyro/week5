package umlang.errors;

/** Exception bearing an InterpretationError. */
public class InterpretationException extends Exception {
    private final Error _error;

    public InterpretationException(Error error) {
        super(error.errorString());
        this._error = error;
    }

    public Error error() {
        return this._error;
    }
}
