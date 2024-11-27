package umlang.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import immutable.Cons;
import immutable.List;
import umlang.ast.*;
import syntax.*;

public class TestParse {
    /** Parse a String into an S-expression and then into an Exp if possible. */
    public static Exp parse(String input) throws IOException {
        return Parser.parse(Term.readFrom(input));
    }

    /** Parse a String into an S-expression and then into a ToplevelClause if possible. */
    public static ToplevelClause parseToplevel(String input) throws IOException {
        return Parser.parseToplevel(Term.readFrom(input));
    }

    private Ref ref(String s) {
        return new Ref(new AtomSymbol(s));
    }

    private void assertParseError(String expected, Executable body) {
        try {
            body.execute();
            fail("No errors parsing, but an error was expected");
        } catch (ParseError e) {
            if (!e.toString().contains(expected)) {
                fail("Expected ParseError message to include \"" + expected + "\", but it was: " + e);
            }
        } catch (Throwable e) {
            fail("" + e);
        }
    }

    @Test void testParseSuccess() throws IOException {
        assertEquals(parse("x"), ref("x"));
        assertEquals(parse("0"), new Num(0));
        assertEquals(parse("#t"), new Bool(true));
        assertEquals(parse("#f"), new Bool(false));
        assertEquals(parse("\"hi\""), new Str("hi"));
        assertEquals(parse("-5.6"), new Num(-5.6));
        assertEquals(parse("{+ 3 4}"), new Call(ref("+"), List.of(new Num(3), new Num(4))));
        assertEquals(parse("{+ 1 {+ 2 3}}"),
            new Call(ref("+"), List.of(new Num(1),
                new Call(ref("+"), List.of(new Num(2), new Num(3))))));
        assertEquals(parse("{if #t 1 2}"),
            new Conditional(new Bool(true), new Num(1), new Num(2)));
        assertEquals(parse("{let {{x 123} {y 234}} {+ x y}}"),
            new Let(
                List.of(new AtomSymbol("x"), new AtomSymbol("y")),
                List.of(new Num(123), new Num(234)),
                new Call(ref("+"), List.of(ref("x"), ref("y")))));
        assertEquals(parse("{fn {x} {+ x 1}}"),
            new Fn(List.of(new AtomSymbol("x")),
                new Call(ref("+"), List.of(ref("x"), new Num(1)))));
        assertEquals(parse("{1 2}"), new Call(new Num(1), List.of(new Num(2))));
        assertEquals(parse("{seq 1 2 3}"),
            new Seq((Cons<Exp>) List.<Exp>of(new Num(1), new Num(2), new Num(3))));
        assertEquals(parse("{obj}"), new InertObj());
        assertEquals(parse("{obj {:foo {} self}}"),
            new ExtendObj(new AtomSymbol(":foo"), List.of(), ref("self"), new InertObj()));
        assertEquals(parse("{obj {:foo {} self} #:base x}"),
            new ExtendObj(new AtomSymbol(":foo"), List.of(), ref("self"), ref("x")));
        assertEquals(parse("{obj {:foo {} self} {:bar {} 123} {:zot {x} x}}"),
            new ExtendObj(new AtomSymbol(":foo"), List.of(), ref("self"),
                new ExtendObj(new AtomSymbol(":bar"), List.of(), new Num(123),
                    new ExtendObj(new AtomSymbol(":zot"), List.of(new AtomSymbol("x")), ref("x"),
                        new InertObj()))));
        assertEquals(parse("{throw 123}"), new Throw(new Num(123)));
        assertEquals(parse("{catch 123 {x} 234}"),
            new TryCatch(new Num(123), new AtomSymbol("x"), new Num(234)));
    }

    @Test void testParseFailure() {
        assertParseError("Bad 'if' syntax", () -> parse("{if #t then 1 else 2}"));
        assertParseError("Bad 'let' syntax", () -> parse("{let {{x 123 234}} x}"));
        assertParseError("Bad 'let' syntax", () -> parse("{let {x} x}"));
        assertParseError("Bad 'seq' syntax", () -> parse("{seq}"));
      
        assertParseError("Bad 'obj' syntax", () -> parse("{obj {f {} 1}}"));
        assertParseError("Bad 'obj' syntax", () -> parse("{obj {:f {\"x\"} 1}}"));
        assertParseError("Bad 'obj' syntax", () -> parse("{obj {:f 1}}"));
        assertParseError("Bad 'obj' syntax", () -> parse("{obj {:f {x} 1} #:base}"));

        assertParseError("Bad 'throw' syntax", () -> parse("{throw}"));
        assertParseError("Bad 'throw' syntax", () -> parse("{throw 123 234}"));

        assertParseError("Bad 'catch' syntax", () -> parse("{catch}"));
        assertParseError("Bad 'catch' syntax", () -> parse("{catch 123}"));
        assertParseError("Bad 'catch' syntax", () -> parse("{catch 123 x 234}"));
        assertParseError("Bad 'catch' syntax", () -> parse("{catch 123 {x}}"));
        assertParseError("Bad 'catch' syntax", () -> parse("{catch 123 234 {x} 345}"));
        assertParseError("Bad 'catch' syntax", () -> parse("{catch 123 {x y} 234}"));
        assertParseError("Bad 'catch' syntax", () -> parse("{catch 123 {x} 234 345}"));
      
        assertParseError("Parse error", () -> parse("{}"));
        assertParseError("Parse error", () -> parse("{obj {:f {x} 1} #:base {}}"));
    }

    @Test void testDefinitionSyntaxErrors() {
        assertParseError("Bad 'define' syntax", () -> parseToplevel("{define}"));
        assertParseError("Bad 'define' syntax", () -> parseToplevel("{define {x y} y}"));
        assertParseError("Bad 'define' syntax", () -> parseToplevel("{define x y z}"));
        assertParseError("Bad 'define' syntax", () -> parseToplevel("{define x}"));
    }

    @Test void testUnparse() throws IOException {
        assertEquals("x", parse("x").unparse().toString());
        assertEquals("0", parse("0").unparse().toString());
        assertEquals("#t", parse("#t").unparse().toString());
        assertEquals("#f", parse("#f").unparse().toString());
        assertEquals("\"hi\"", parse("\"hi\"").unparse().toString());
        assertEquals("-5.6", parse("-5.6").unparse().toString());
        assertEquals("{+ 3 4}", parse("{+ 3 4}").unparse().toString());
        assertEquals("{+ 1 {+ 2 3}}", parse("{+ 1 {+ 2 3}}").unparse().toString());
        assertEquals("{if #t 1 2}", parse("{if #t 1 2}").unparse().toString());
        assertEquals("{let {{x 123} {y 234}} {+ x y}}", parse("{let {{x 123} {y 234}} {+ x y}}").unparse().toString());
        assertEquals("{fn {x} {+ x 1}}", parse("{fn {x} {+ x 1}}").unparse().toString());
        assertEquals("{1 2}", parse("{1 2}").unparse().toString());
        assertEquals("{seq 1 2 3}", parse("{seq 1 2 3}").unparse().toString());
        //assertEquals("{obj}", parse("{obj}").unparse().toString());
        // assertEquals("{obj {:foo {} self} #:base {obj}}", parse("{obj {:foo {} self}}").unparse().toString());
        // assertEquals("{obj {:foo {} self} #:base x}", parse("{obj {:foo {} self} #:base x}").unparse().toString());
        assertEquals(
            "{obj {:foo {} self} #:base {obj {:bar {} 123} #:base {obj {:zot {x} x} #:base {obj}}}}",
            parse("{obj {:foo {} self} {:bar {} 123} {:zot {x} x}}").unparse().toString());
    }
}
