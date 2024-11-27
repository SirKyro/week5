package syntax;

/** Represents an S-expression number, here simply a double */
public record AtomNumber(double value) implements Atom {
    @Override
    public String toString() {
        if (value == (long) value) {
            return "" + (long) value;
        } else {
            return "" + value;
        }
    }
}
