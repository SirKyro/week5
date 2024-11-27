package umlang;

import java.util.HashMap;
import java.util.Map;

import immutable.List;
import immutable.Maybe;
import immutable.None;
import immutable.Some;
import immutable.ThrowingFunction;
import syntax.AtomSymbol;
import umlang.errors.InterpretationException;
import umlang.errors.UserException;
import umlang.value.VBool;
import umlang.value.VCell;
import umlang.value.VNum;
import umlang.value.VPrim;
import umlang.value.VStr;
import umlang.value.Value;

public class GlobalEnvironment<X> {
    /**
     * A name is either
     * - missing: it has not been introduced at all.
     * - bound to None: it has been declared, but its value is not yet ready.
     * - bound to Some(v): it has been declared, and v is its value.
     */
    private Map<AtomSymbol, Maybe<X>> globals = new HashMap<>();

    /** Retrieve a value matching `name`, if any. */
    public Maybe<Maybe<X>> lookup(AtomSymbol name) {
        var binding = globals.get(name);
        return binding == null ? new None<>() : new Some<>(binding);
    }

    /** Declare a top-level/global variable `name`. Its value will come later. */
    public void declare(AtomSymbol name) {
        if (!globals.containsKey(name)) {
            globals.put(name, new None<>());
        }
    }

    /** Update a top-level/global variable `name` to be `value`. */
    public void set(AtomSymbol name, X value) {
        if (!globals.containsKey(name)) {
            throw new IllegalStateException("Attempted to set variable before declaring it: " + name);
        }
        globals.put(name, new Some<>(value));
    }

    /** Helper: constructs a VPrim and wraps it in an Entry ready for use in an Environment<Value>. See PRIM_ENV */
    private static Entry<VPrim> prim(String name, int arity, ThrowingFunction<List<Value>, Value, InterpretationException> proc) {
        return new Entry<>(new AtomSymbol(name), new VPrim(arity, proc));
    }

    /** Primitives. */
    public static Environment<VPrim> PRIMITIVES = new Environment<>(List.of(
        // + : Any Any -> Number
        // Adds the two numbers. SAFETY: errors if it gets non-numbers.
        prim("+", 2, (vs) -> new VNum(vs.get(0).asNum() + vs.get(1).asNum())),

        // - : Any Any -> Number
        // Subtracts the two numbers. SAFETY: errors if it gets non-numbers.
        prim("-", 2, (vs) -> new VNum(vs.get(0).asNum() - vs.get(1).asNum())),

        // * : Any Any -> Number
        // Multiplies the two numbers. SAFETY: errors if it gets non-numbers.
        prim("*", 2, (vs) -> new VNum(vs.get(0).asNum() * vs.get(1).asNum())),

        // / : Any Any -> Number
        // Divides the two numbers. SAFETY: errors if it gets non-numbers, or denominator is zero
        prim("/", 2, (vs) -> {
            var denominator = vs.get(1).asNum();
            if (denominator == 0) throw new InterpretationException(new UserException(new VStr("division-by-zero")));
            return new VNum(vs.get(0).asNum() / denominator);
        }),

        // = : Any Any -> Bool
        // Answers #t iff the arguments are equal to one another.
        prim("=", 2, (vs) -> {
            var a = vs.get(0);
            var b = vs.get(1);
            return new VBool(a.isStructuralEquivalenceAppropriate() && b.isStructuralEquivalenceAppropriate() && a.equals(b));
        }),

        //----------------------------------------------------------------
        // I/O

        // display : Any -> 0
        prim("display", 1, (vs) -> {
            System.out.print(vs.get(0).toDisplayableString());
            return new VNum(0);
        }),
        // newline : -> 0
        prim("newline", 0, (vs) -> {
            System.out.print("\n");
            return new VNum(0);
        }),

        //----------------------------------------------------------------
        // Cells

        // cell : Any -> Cell
        // Allocate a fresh cell.
        prim("cell", 1, (vs) -> new VCell(vs.get(0))),

        // get : Any -> Any
        // Extract the current value of the cell in the first argument. SAFETY: errors if it gets a non-cell.
        prim("get", 1, (vs) -> vs.get(0).asCell().contents()),

        // set : Any Any -> Any
        // Update the current value of the cell in the first argument. Return the old value. SAFETY: errors if it gets a non-cell.
        prim("set", 2, (vs) -> {
            var c = vs.get(0).asCell();
            var oldValue = c.contents();
            c.setContents(vs.get(1));
            return oldValue;
        })
    ));
}
