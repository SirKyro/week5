package syntax;

/** Represents an S-expression "symbol", which has an interned label */
public record AtomSymbol(String label) implements Atom {
    public AtomSymbol {
        label = label.intern();
    }

    @Override
    public String toString() {
        return label;
    }
}
