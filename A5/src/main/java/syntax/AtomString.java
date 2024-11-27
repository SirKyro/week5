package syntax;

/** Represents an S-expression string */
public record AtomString(String text) implements Atom {
    @Override
    public String toString() {
        var s = text;
        s = s.replace("\\", "\\\\");
        s = s.replace("\"", "\\\"");
        return "\"" + s + "\"";
    }
}
