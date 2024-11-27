package umlang;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import immutable.List;
import immutable.Maybe;
import immutable.None;
import immutable.Some;
import syntax.AtomSymbol;
import syntax.Term;
import syntax.TermReader;
import umlang.ast.Definition;
import umlang.ast.Exp;
import umlang.ast.ParseError;
import umlang.ast.Parser;
import umlang.ast.ToplevelClause;
import umlang.errors.InterpretationException;
import umlang.errors.UnboundVariable;
import umlang.errors.UninitializedGlobal;
import umlang.value.Value;
import umlang.vm.VM;

/** An Interpreter is a self-contained interpreter for umlang `Exp`s. */
public abstract class Interpreter {
    private final GlobalEnvironment<Value> _globals;

    public Interpreter() {
        this._globals = new GlobalEnvironment<>();
    }

    public Interpreter(GlobalEnvironment<Value> globals) {
        this._globals = globals;
    }

    public GlobalEnvironment<Value> globals() {
        return _globals;
    }

    /** Compute the result of `exp` in the lexical environment `env`, extended with `globals` and `GlobalEnvironment.PRIMITIVES`. */
    public abstract Value evaluate(Exp exp, Environment<Value> env) throws InterpretationException;

    /** Compute the result of `exp` in an empty lexical environment, extended with `globals` and `GlobalEnvironment.PRIMITIVES`. */
    public Value evaluate(Exp exp) throws InterpretationException {
        return evaluate(exp, new Environment<>());
    }

    /** Parse the string into a single `Exp`, then evaluate the result. */
    public Value evaluate(String sourceCode) throws IOException, InterpretationException {
        Exp exp;
        try {
            exp = Parser.parse(Term.readFrom(sourceCode));
        } catch (ParseError pe) {
            throw new IOException(pe);
        }
        return evaluate(exp);
    }

    /** Evaluate a method call. */
    public abstract Value callMethod(Value receiver, AtomSymbol selector, List<Value> arguments) throws InterpretationException;

    /** Retrieve a fresh "default" interpreter instance */
    public static Interpreter newDefault() {
        var override = System.getenv("UMLANG_VM");
        return (override != null && override.equals("1")) ? new VM() : new RecursiveInterpreter();
    }

    /** Parse `sourceCode` into a list of ToplevelClause, then call evaluateProgram on that list. */
    public Maybe<Value> evaluateProgram(String sourceCode) throws InterpretationException, ParseError, IOException {
        return evaluateProgram(new TermReader(sourceCode).readAll().terms().map(Parser::parseToplevel));
    }

    /** Parse the contents of `r` into a list of ToplevelClause, then call evaluateProgram on that list. */
    public Maybe<Value> evaluateProgram(Reader r) throws InterpretationException, ParseError, IOException {
        return evaluateProgram(new TermReader(r).readAll().terms().map(Parser::parseToplevel));
    }

    /** Load the umlang program in file `filename` into this interpreter, yielding its final result, if any. */
    public Maybe<Value> loadProgram(String filename) throws ParseError, InterpretationException, IOException {
        try (var f = new InputStreamReader(new FileInputStream(filename))) {
            return this.evaluateProgram(f);
        }
    }

    /** Declare all definitions, then run all definitions and/or expressions in `program`, one after the other. */
    public Maybe<Value> evaluateProgram(List<ToplevelClause> program) throws InterpretationException {
        // First declare all definitions.
        for (var c : program) {
            if (c instanceof Definition def) {
                _globals.declare(def.name());
            }
        }

        // Then execute everything.
        Maybe<Value> result = new None<>();
        for (var c : program) {
            switch (c) {
                case Definition(var name, var initializer) -> {
                    _globals.set(name, evaluate(initializer));
                    result = new None<>();
                }
                case Exp e -> result = new Some<>(evaluate(e));
            }
        }

        return result;
    }

    /** Look up `name` in `env`; if it is absent, look in `globals`; if absent there too, look in `GlobalEnvironment.PRIMITIVES`. */
    protected Value lookup(AtomSymbol name, Environment<Value> env) throws InterpretationException {
        return env.lookup(name)
            .orElse(() -> _globals.lookup(name).map((maybeValue) -> maybeValue.valueOr(() -> new UninitializedGlobal(name).signal())))
            .orElse(() -> GlobalEnvironment.PRIMITIVES.lookup(name))
            .valueOr(() -> new UnboundVariable(name).signal());
    }
}
