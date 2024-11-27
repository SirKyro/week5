package umlang.ast;

import syntax.Term;

public class ParseError extends RuntimeException {
    public ParseError(String message, Term irritant) {
        super(message + ": " + irritant);
    }

    /** Convenience method for throwing in expression context */
    public <Y> Y signal() {
        throw this;
    }
}