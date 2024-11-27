package syntax;

import java.util.Arrays;

/** A Form represents a compound S-expression: a sequence of smaller S-expressions. */
public record Form(immutable.List<? extends Term> terms) implements Term {
    // Used by toString() to print Form instances. INVARIANT: must be at least two characters long.
    public static final ThreadLocal<String> PARENS_FOR_PRINTING = new ThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return "{}";
        };
    };

    public Form(Term... terms) {
        this(immutable.List.fromList(Arrays.asList(terms)));
    }

    @Override
    public String toString() {
        var pp = PARENS_FOR_PRINTING.get();
        return pp.charAt(0) + terms.fold("", (t, s) -> t.toString() + (s.isEmpty() ? s : " " + s)) + pp.charAt(1);
    }
}
