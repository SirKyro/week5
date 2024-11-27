package umlang.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import immutable.List;
import immutable.ThrowingConsumer;
import syntax.AtomSymbol;
import umlang.Entry;
import umlang.Environment;
import umlang.Interpreter;
import umlang.ast.*;
import umlang.errors.ExpectedBool;
import umlang.errors.ExpectedCell;
import umlang.errors.ExpectedFn;
import umlang.errors.ExpectedNum;
import umlang.errors.ExpectedObj;
import umlang.errors.BadArgumentCount;
import umlang.errors.Error;
import umlang.errors.InterpretationException;
import umlang.errors.MethodNotFound;
import umlang.errors.UnboundVariable;
import umlang.errors.UninitializedGlobal;
import umlang.errors.UserException;
import umlang.value.*;

public class TestInterp {
    // By default, junit jupiter creates a new instance of TestInterp for each @Test-annotated method.
    // That means that each test gets a fresh `interpreter`.
    private Interpreter interpreter = Interpreter.newDefault();

    private Ref ref(String s) {
        return new Ref(new AtomSymbol(s));
    }

    private String checkRun(String input, Value expected) {
        return checkRun(input, expected, Assertions::fail);
    }

    private String checkRun(String input, Value expected, ThrowingConsumer<Throwable, RuntimeException> onExn) {
        var collector = new ByteArrayOutputStream();
        var saved = System.out;
        System.setOut(new PrintStream(collector));
        try {
            assertEquals(expected, interpreter.evaluate(input));
        } catch (Throwable t) {
            onExn.accept(t);
        } finally {
            System.setOut(saved);
        }
        return collector.toString();
    }

    private Error runToError(String input) {
        return runToError(() -> interpreter.evaluate(input));
    }

    private Error runToError(Executable thunk) {
        try {
            thunk.execute();
            fail("Expected an interpretation error, but none was signalled");
            throw new IllegalStateException("unreachable");
        } catch (InterpretationException e) {
            return e.error();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void checkRunError(String input, Error expected) {
        assertEquals(runToError(input), expected);
    }

    @Test void testSimpleInterpretation() {
        checkRun("0", new VNum(0));
        checkRun("#t", new VBool(true));
        checkRun("\"hi\"", new VStr("hi"));
        checkRun("-5.6", new VNum(-5.6));
        checkRun("{+ 3 4}", new VNum(7));
        checkRun("{+ 1 {+ 2 3}}", new VNum(6));
        checkRun("{let {{x 123} {y 234}} {+ x y}}", new VNum(357));
        checkRun("{fn {x} {+ x 1}}",
            new VFn(List.of(new AtomSymbol("x")),
                new Call(ref("+"), List.of(ref("x"), new Num(1))),
                new Environment<>()));
        checkRun("{{fn {x} {+ x 1}} 123}", new VNum(124));
        checkRun("{{fn {x y} {+ x y}} 123 234}", new VNum(357));
        checkRun("{obj {:f {x} 1}}", new VObj(new Environment<>(List.of(
            new Entry<>(new AtomSymbol(":f"),
                new VFn(List.of(new AtomSymbol("self"), new AtomSymbol("x")),
                    new Num(1),
                    new Environment<>()))))));
    }

    @Test void testTruthiness() throws InterpretationException {
        checkRun("{if #t 1 2}", new VNum(1));
        checkRun("{if #f 1 2}", new VNum(2));
        checkRunError("{if -1 1 2}", new ExpectedBool(new VNum(-1)));
        checkRunError("{if 100 1 2}", new ExpectedBool(new VNum(100)));
        checkRunError("{if 0 1 2}", new ExpectedBool(new VNum(0)));
        checkRunError("{if {fn {} #t} 1 2}", new ExpectedBool(new VFn(List.of(), new Bool(true), new Environment<>())));
        checkRunError("{if {obj} 1 2}", new ExpectedBool(new VObj(new Environment<>(List.of()))));
        checkRunError("{if \"hi\" 1 2}", new ExpectedBool(new VStr("hi")));
        checkRunError("{if \"\" 1 2}", new ExpectedBool(new VStr("")));

        // See discussion in testCells() for why the next couple of cases look very different!

        var e1 = runToError("{if {cell #t} 1 2}");
        assertInstanceOf(ExpectedBool.class, e1);
        assertInstanceOf(VCell.class, ((ExpectedBool) e1).actual());
        assertEquals(new VBool(true), ((ExpectedBool) e1).actual().asCell().contents());

        var e2 = runToError("{if {cell #f} 1 2}");
        assertInstanceOf(ExpectedBool.class, e2);
        assertInstanceOf(VCell.class, ((ExpectedBool) e2).actual());
        assertEquals(new VBool(false), ((ExpectedBool) e2).actual().asCell().contents());
    }

    @Test void testObjectDelegation() throws IOException {
        checkRun("""
            {let {{b {obj {:m {x} {+ x 1}}}}}
              {let {{o {obj {:m {x} {+ x 2}}}}}
                {b :m 123}}}""",
            new VNum(124));

        checkRun("""
            {let {{b {obj {:m {x} {+ x 1}}}}}
              {let {{o {obj {:m {x} {+ x 2}}}}}
                {o :m 123}}}""",
            new VNum(125));

        checkRun("""
            {let {{b {obj {:m {x} {+ x 1}}}}}
              {let {{o {obj {:q {x} {+ x 2}} #:base b}}}
                {o :m 123}}}""",
            new VNum(124));
    }

    @Test void testCells() throws IOException, InterpretationException {
        // We discussed *structural* vs *behavioural* equivalences in class.
        // With stateless, immutable, pure *data*, we can use structural equivalences - `equal?`, `equals()` etc.
        // With stateful, mutable references, we cannot. We must use *observational*, behavioural equivalences instead.

        // So it's totally reasonable to *structurally* check the results of an *observation* of some
        // stateful object.
        checkRun("{get {cell 123}}", new VNum(123));
        checkRun("{set {cell 123} 234}", new VNum(123));
        checkRun("""
            {let {{c {cell 123}}}
              {seq {set c 234}
                {get c}}}""", new VNum(234));
        checkRun("""
            {let {{c {cell 0}}}
              {let {{inc {fn {} {set c {+ {get c} 1}}}}}
                {seq {inc}
                     {inc}
                     {inc}
                     {get c}}}}""", new VNum(3));

        // But cells have *identity*: new VCell(...) != new VCell(...)
        // So this test would always fail if we tried it:
        //
        // checkRun("{cell 123}", new VCell(new VNum(123)));
        //
        // Instead, we check that we have something of the right type,
        // and then make our own observations of it. Very similar to the
        // tests above, which made the observations *inside* the umlang
        // language; here, we make observations *outside* umlang, using
        // Java, which is the *implementing* language, not the *implemented*
        // language.
        var v = interpreter.evaluate("{cell 123}");
        assertInstanceOf(VCell.class, v);
        assertEquals(v.asCell().contents(), new VNum(123));
    }

    @Test void testScoping() throws IOException {
        // These distinguish between dynamic and static scoping.

        checkRun("""
            {let {{x 1}}
              {let {{add-x-to {fn {y} {+ x y}}}}
                {add-x-to 10}}}""", new VNum(11));

        checkRun("""
            {let {{x 1}}
              {let {{add-x-to {fn {y} {+ x y}}}}
                {let {{x 2}}
                  {add-x-to 10}}}}""", new VNum(11));

        checkRunError("""
            {let {{add-x-to {fn {y} {+ x y}}}}
              {let {{x 2}}
                {add-x-to 10}}}""",
            new UnboundVariable(new AtomSymbol("x")));
    }

    @Test void testAnimalExample() throws IOException {
        var output = checkRun("""
            {let {{animal {obj {:greet {who} {seq {display "Hello, "}
                                                  {display who}
                                                  {newline}
                                                  {self :speak}}}}}}
                {let {{cat {obj {:speak {} {display "Meow!\n"}} #:base animal}}
                      {dog {obj {:speak {} {display "Woof!\n"}} #:base animal}}}
                    {seq {cat :greet "world"}
                         {dog :greet "world"}}}}
                """, new VNum(0));
        assertEquals("Hello, world\nMeow!\nHello, world\nWoof!\n", output);
    }

    @Test void testCounterExample() throws IOException {
        checkRun("""
                {let {{make-counter {fn {}
                                        {let {{count {cell 0}}}
                                            {obj {:get {} {get count}}
                                                {:inc {} {set count {+ {get count} 1}}}}}}}}
                    {let {{c {make-counter}}}
                        {seq {c :inc}
                            {c :inc}
                            {c :get}}}}
                """, new VNum(2));
    }

    @Test void testObjectListsExample() throws IOException, InterpretationException {
        var result = interpreter.evaluate("""
                {let {{nil {obj {:map {f} self}}}
                      {cons {obj {:new {first rest}
                                     {let {{cons self}}
                                           {obj {:map {f} {cons :new {f first} {rest :map f}}}
                                                {:first {} first}
                                                {:rest {} rest}}}}}}}
                  {{cons :new 1 {cons :new 2 {cons :new 3 nil}}} :map {fn {val} {+ val 1}}}}
                """);
        assertInstanceOf(VObj.class, result);
        assertEquals(new VNum(2), interpreter.callMethod(result, new AtomSymbol(":first"), List.empty()));
        result = interpreter.callMethod(result, new AtomSymbol(":rest"), List.empty());
        assertEquals(new VNum(3), interpreter.callMethod(result, new AtomSymbol(":first"), List.empty()));
        result = interpreter.callMethod(result, new AtomSymbol(":rest"), List.empty());
        assertEquals(new VNum(4), interpreter.callMethod(result, new AtomSymbol(":first"), List.empty()));
        final var last = interpreter.callMethod(result, new AtomSymbol(":rest"), List.empty());
        assertEquals(new MethodNotFound(new AtomSymbol(":first")), runToError(() -> interpreter.callMethod(last, new AtomSymbol(":first"), List.empty())));
    }

    @Test void testMethodNotFound() {
        checkRunError("{{obj} :no}", new MethodNotFound(new AtomSymbol(":no")));
        checkRunError("{{obj {:yes {} 1}} :no}", new MethodNotFound(new AtomSymbol(":no")));
        checkRunError("{{obj {:yes {} 1} #:base {obj {:alsoYes {} 2}}} :no}", new MethodNotFound(new AtomSymbol(":no")));
    }

    @Test void testThrowCatch() {
        checkRun("{catch {+ {throw 123} 234} {exn} exn}", new VNum(123));
        checkRun("{catch {+ 123 234} {exn} exn}", new VNum(357));
        checkRunError("{throw 123}", new UserException(new VNum(123)));
    }

    @Test void testBadArgumentCount() {
        checkRunError("{+ 1 2 3}", new BadArgumentCount(2, 3));
        checkRunError("{+}", new BadArgumentCount(2, 0));
        checkRunError("{{fn {x} 0} 1 2 3}", new BadArgumentCount(1, 3));
        checkRunError("{{fn {x} 0}}", new BadArgumentCount(1, 0));
    }

    @Test void testVariousExpecteds() {
        checkRunError("{get 123}", new ExpectedCell(new VNum(123)));
        checkRunError("{set 123 234}", new ExpectedCell(new VNum(123)));
        checkRunError("{123}", new ExpectedFn(new VNum(123)));
        checkRunError("{+ #f #t}", new ExpectedNum(new VBool(false)));
        checkRunError("{123 :add 1 2}", new ExpectedObj(new VNum(123)));
    }

    @Test void testUninitializedGlobal1() throws ParseError, InterpretationException, IOException {
        assertEquals(new UninitializedGlobal(new AtomSymbol("f")), runToError(() -> interpreter.evaluateProgram("{define f {f}}")));
    }

    /** Compare with testUninitializedGlobal3 */
    @Test void testUninitializedGlobal2() throws ParseError, InterpretationException, IOException {
        assertEquals(new UninitializedGlobal(new AtomSymbol("f")), runToError(() -> interpreter.evaluateProgram("{define g {fn {} {f}}} {define f {g}}")));
    }

    /** Compare with testUninitializedGlobal2 */
    @Test void testUninitializedGlobal3() throws ParseError, InterpretationException, IOException {
        assertEquals(new UnboundVariable(new AtomSymbol("f")), runToError(() -> interpreter.evaluateProgram("{define g {fn {} {f}}} {g}")));
    }

    @Test void testRecursiveProgram() throws ParseError, InterpretationException, IOException {
        assertEquals(new VBool(true), interpreter.evaluateProgram("""
            {define zero? {fn {x} {= x 0}}}
            {define even? {fn {x} {if {zero? x} #t {odd? {- x 1}}}}}
            {define odd? {fn {x} {if {zero? x} #f {even? {- x 1}}}}}
            {odd? 11}
            """).unwrap());
    }

    @Test void testArithmetic() {
        checkRun("{+ 3 4}", new VNum(7));
        checkRun("{- 3 4}", new VNum(-1));
        checkRun("{* 3 4}", new VNum(12));
        checkRun("{/ 3 4}", new VNum(0.75));
    }

    @Test void testEquality() {
        checkRun("{= 0 1}", new VBool(false));
        checkRun("{= 0 0}", new VBool(true));
        checkRun("{= 1 1}", new VBool(true));
        checkRun("{= {fn {x} x} \"hello\"}", new VBool(false));
        checkRun("{= {fn {x} x} {fn {x} x}}", new VBool(false));
        checkRun("{= {fn {x} x} {obj}}", new VBool(false));
        checkRun("{= {obj} {obj}}", new VBool(false));
        checkRun("{= {cell 1} {cell 1}}", new VBool(false));
    }

    @Test void testDivisionByZero() {
        checkRun("{catch {/ 1 0} {exn} exn}", new VStr("division-by-zero"));
    }
}
