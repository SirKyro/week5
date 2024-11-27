package syntax;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import immutable.List;
import immutable.Maybe;
import immutable.None;
import immutable.Some;

/** A Term represents an S-expression: either an Atom, or a Form */
public sealed interface Term permits Atom, Form {
    /** Parse a Term from the given input stream. null for end-of-stream */
    static Term readFrom(Reader r) throws IOException {
        return new TermReader(r).next();
    }

    /** Parse a Term from the given string. null for end-of-stream */
    static Term readFrom(String input) throws IOException {
        return readFrom(new StringReader(input));
    }

    /** Answer None unless `this` is an `AtomNumber` */
    default Maybe<Double> number() {
        return switch (this) {
            case AtomNumber(var n) -> new Some<>(n);
            default -> new None<>();
        };
    }

    /** Answer None unless `this` is an `AtomString` */
    default Maybe<String> string() {
        return switch (this) {
            case AtomString(var s) -> new Some<>(s);
            default -> new None<>();
        };
    }

    /** Answer None unless `this` is an `AtomSymbol` */
    default Maybe<AtomSymbol> symbol() {
        return switch (this) {
            case AtomSymbol s -> new Some<>(s);
            default -> new None<>();
        };
    }

    /** Answer None unless `this` is an `AtomString` */
    default Maybe<List<? extends Term>> form() {
        return switch (this) {
            case Form(var terms) -> new Some<>(terms);
            default -> new None<>();
        };
    }
}