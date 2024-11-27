package syntax;

import java.io.IOException;

/** Thrown to signal a syntax error during TermReader's operation. */
public class TermSyntaxError extends IOException {
    public TermSyntaxError(String message) {
        super(message);
    }
}
