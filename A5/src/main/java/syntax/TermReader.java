package syntax;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Pattern;

/** A stateful object representing the operation of reading a Term from a java.io.Reader. */
public class TermReader {
    /** The underlying input. Not accessed directly: accessed via `buffer` */
    private final Reader reader;
    /** A single-character buffer. `-2` means "empty"; `-1` means "end of input"; nonnegative is a character, as per Reader.read(). */
    private int buffer = -2;
    
    /** Construct a Reader from a Readable. */
    public TermReader(Reader r) {
        this.reader = r;
        this.reset();
    }

    /** Construct a Reader from a string of input. */
    public TermReader(String input) {
        this(new StringReader(input));
    }

    /** Retrieve the next Term from the underlying reader, or null if the end of stream has been reached.
     * Recursive, and the recursion is driven by the input; risk of stack overflow.
     * Throws TermSyntaxError on syntax errors.
     */
    public Term next() throws IOException {
        skipWhitespace();
        return switch (peek()) {
            case -1 -> null;
            case '(' -> { skip(); yield readForm(')'); }
            case '[' -> { skip(); yield readForm(']'); }
            case '{' -> { skip(); yield readForm('}'); }
            case ']' -> throw new TermSyntaxError("Unexpected close-bracket");
            case '}' -> throw new TermSyntaxError("Unexpected close-brace");
            case ')' -> throw new TermSyntaxError("Unexpected close-parenthesis");
            case '"' -> { skip(); yield readString(); }
            default -> readSymbolOrNumber();
        };
    }

    /** Read all remaining Terms from the input, answering a Form containing them all in order. */
    public Form readAll() throws IOException {
        immutable.List<Term> terms = new immutable.Nil<>();
        while (true) {
            var term = next();
            if (term == null) break;
            terms = new immutable.Cons<>(term, terms);
        }
        return new Form(terms.reverse());
    }

    private Term readForm(char expectedCloseParen) throws IOException {
        immutable.List<Term> terms = new immutable.Nil<>();
        while (true) {
            skipWhitespace();
            int ch = peek();
            if (ch == -1) break;
            if (ch == expectedCloseParen) {
                skip();
                return new Form(terms.reverse());
            }
            var term = next();
            if (term == null) break;
            terms = new immutable.Cons<>(term, terms);
        }
        throw new TermSyntaxError("Missing close-parenthesis at end of input");
    }

    private AtomString readString() throws IOException {
        var b = new StringBuilder();
        while (true) {
            int ch = nextChar();
            if (ch == -1) throw new TermSyntaxError("Missing close-quote at end of input in string");
            if (ch == '"') return new AtomString(b.toString());
            if (ch == '\\') {
                ch = nextChar();
                if (ch == -1) throw new TermSyntaxError("Missing character after backslash escape in string");
            }
            b.append((char) ch);
        }
    }

    private static boolean isDelimiter(int ch) {
        return Character.isWhitespace(ch) ||
            ch == '(' || ch == ')' ||
            ch == '[' || ch == ']' ||
            ch == '{' || ch == '}' ||
            ch == ';' || ch == '"' || ch == -1;
    }

    private static final Pattern NUMBER_RE = Pattern.compile("([-+]?\\d+)((\\.\\d+([eE][-+]?\\d+)?)|([eE][-+]?\\d+))?");

    private Atom readSymbolOrNumber() throws IOException {
        var b = new StringBuilder();
        while (true) {
            int ch = peek();
            if (TermReader.isDelimiter(ch)) break;
            b.append((char) ch);
            skip();
        }
        var s = b.toString();
        if (NUMBER_RE.matcher(s).matches()) {
            return new AtomNumber(Double.parseDouble(s));
        } else {
            return new AtomSymbol(s);
        }
    }

    private int peek() throws IOException {
        if (buffer == -2) buffer = reader.read();
        return buffer;
    }

    private void skip() throws IOException {
        if (buffer == -2) peek();
        buffer = -2;
    }

    private int nextChar() throws IOException {
        int r = peek();
        skip();
        return r;
    }

    private void skipLine() throws IOException {
        while (true) {
            int ch = nextChar();
            if (ch == -1 || ch == '\n') return;
        }
    }

    private void skipWhitespace() throws IOException {
        while (true) {
            int ch = peek();
            if (ch == -1) {
                return;
            }
            if (ch == ';') {
                skip();
                skipLine();
                continue;
            }
            if (!Character.isWhitespace(ch)) {
                return;
            }
            skip();
        }
    }

    /**
     * Throw away internal state, e.g. after a TermSyntaxError, to resume parsing without
     * buffered potentially-erroneous input and to guarantee some kind of progress, even
     * though we cannot any longer guarantee correct parses. */
    public void reset() {
        this.buffer = -2;
    }
}
