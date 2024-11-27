package syntax.tests;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import immutable.List;
import immutable.None;
import immutable.Some;
import syntax.*;

import static org.junit.jupiter.api.Assertions.*;

public class Tests {
    @Test void testTermReaderEmpty() throws IOException {
        assertNull(Term.readFrom(""));
    }

    @Test void testTermReaderEmptyList() throws IOException {
        assertEquals(Term.readFrom("()"), new Form());
    }

    @Test void testStringToString() {
        assertEquals(
            new AtomString("hi \"there\" you!").toString(),
            "\"hi \\\"there\\\" you!\"");
    }

    @Test void testTermAs() throws IOException {
        assertEquals(Term.readFrom("abc").number(), new None<Double>());
        assertEquals(Term.readFrom("123").number(), new Some<>(123.0));
        assertEquals(Term.readFrom("(abc)").number(), new None<Double>());

        assertEquals(Term.readFrom("abc").symbol(), new Some<>(new AtomSymbol("abc")));
        assertEquals(Term.readFrom("123").symbol(), new None<AtomSymbol>());
        assertEquals(Term.readFrom("(abc)").symbol(), new None<AtomSymbol>());

        assertEquals(Term.readFrom("abc").string(), new None<String>());
        assertEquals(Term.readFrom("\"abc\"").string(), new Some<>("abc"));
        assertEquals(Term.readFrom("(abc)").number(), new None<Double>());

        assertEquals(Term.readFrom("abc").form(), new None<List<Term>>());
        assertEquals(Term.readFrom("\"abc\"").form(), new None<List<Term>>());
        assertEquals(Term.readFrom("(abc)").form(), new Some<List<Term>>(List.of(new AtomSymbol("abc"))));
    }

    @Test void testTermReaderNumbersList() throws IOException {
        assertEquals(Term.readFrom("(1 0 -3.45e-2 6600)"), new Form(
            new AtomNumber(1),
            new AtomNumber(0),
            new AtomNumber(-3.45e-2),
            new AtomNumber(6600)
        ));
    }

    @Test void testTermReaderSymbols() throws IOException {
        assertEquals(Term.readFrom("(+ 1 (* 2 3))"), new Form(
            new AtomSymbol("+"),
            new AtomNumber(1),
            new Form(
                new AtomSymbol("*"),
                new AtomNumber(2),
                new AtomNumber(3)
            )
        ));
    }

    @Test void testTermReaderStrings() throws IOException {
        assertEquals(Term.readFrom("(string-append \"hello, \" \"\" \"world!\")"), new Form(
            new AtomSymbol("string-append"),
            new AtomString("hello, "),
            new AtomString(""),
            new AtomString("world!")
        ));
    }

    @Test void testTermReaderComment() throws IOException {
        assertEquals(Term.readFrom("; comment\n123"), new AtomNumber(123));
        assertNull(Term.readFrom("; comment\n   "));
    }

    @Test void testTermReaderSequence() throws IOException {
        assertEquals(new TermReader("(a b c);()\n()1 hi 2(())").readAll(), new Form(
            new Form(new AtomSymbol("a"), new AtomSymbol("b"), new AtomSymbol("c")),
            new Form(),
            new AtomNumber(1),
            new AtomSymbol("hi"),
            new AtomNumber(2),
            new Form(new Form())
        ));
    }

    @Test void testTermReaderVariousParens() throws IOException {
        assertEquals(Term.readFrom("([{}]{()[]})"), new Form(
            new Form(new Form()),
            new Form(new Form(), new Form())
        ));
    }

    @Test void testTermReaderMismatchedParens() {
        assertThrows(TermSyntaxError.class, () -> Term.readFrom("([)]"));
        assertThrows(TermSyntaxError.class, () -> Term.readFrom("[(])"));
        assertThrows(TermSyntaxError.class, () -> Term.readFrom("({)}"));
        assertThrows(TermSyntaxError.class, () -> Term.readFrom("{(})"));
        assertThrows(TermSyntaxError.class, () -> Term.readFrom("{[}]"));
        assertThrows(TermSyntaxError.class, () -> Term.readFrom("[{]}"));
    }

    @Test void testTermReaderUnterminatedList() {
        assertThrows(TermSyntaxError.class, () -> Term.readFrom("("));
        assertThrows(TermSyntaxError.class, () -> Term.readFrom("((("));
        assertThrows(TermSyntaxError.class, () -> Term.readFrom("(()"));
    }

    @Test void testToString() throws IOException {
        assertEquals("{{{}} {{} {}}}", Term.readFrom("([{}]{()[]})").toString());
        assertEquals("\"hi\\\\\\\"foo\"", new AtomString("hi\\\"foo").toString());
        assertEquals("4", new AtomNumber(4).toString());
        assertEquals("4.3", new AtomNumber(4.3).toString());
    }
}
