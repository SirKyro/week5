package umlang.value;

import immutable.List;
import immutable.ThrowingBiFunction;
import immutable.ThrowingFunction;
import syntax.Form;
import umlang.Environment;
import umlang.ast.Exp;
import umlang.errors.BadArgumentCount;
import umlang.errors.ExpectedBool;
import umlang.errors.ExpectedCell;
import umlang.errors.ExpectedFn;
import umlang.errors.ExpectedNum;
import umlang.errors.ExpectedObj;
import umlang.errors.InterpretationException;

public sealed interface Value permits
    VNum,
    VBool,
    VStr,
    VPrim,
    VCell,
    VFn,
    VObj
{
    /** Answer true if a structural equivalence check is appropriate for this kind of value, and false otherwise. Overridden in some implementations! */
    default boolean isStructuralEquivalenceAppropriate() {
        return true;
    }

    /** Signals ExpectedBool unless `this` is suitable for use in a conditional */
    default boolean isTruthy() throws InterpretationException {
        return switch (this) {
            case VBool(var v) -> v;
            default -> new ExpectedBool(this).signal();
        };
    }

    /** Signals ExpectedObj unless `this` is a VObj */
    default VObj asObj() throws InterpretationException {
        return switch (this) {
            case VObj v -> v;
            default -> new ExpectedObj(this).signal();
        };
    }

    /** Signals ExpectedNum unless `this` is a VNum */
    default double asNum() throws InterpretationException {
        return switch (this) {
            case VNum(var n) -> n;
            default -> new ExpectedNum(this).signal();
        };
    }

    /** Signals ExpectedCell unless `this` is a VCell */
    default VCell asCell() throws InterpretationException {
        return switch (this) {
            case VCell c -> c;
            default -> new ExpectedCell(this).signal();
        };
    }

    default <Y> Y invoke(
        List<Value> argValues,
        ThrowingBiFunction<Exp, Environment<Value>, Y, InterpretationException> ifFn,
        ThrowingFunction<Value, Y, InterpretationException> ifPrimValue,
        ThrowingFunction<InterpretationException, Y, InterpretationException> ifPrimError
    ) throws InterpretationException {
        return switch (this) {
            case VFn(var formals, var body, var env) -> {
                if (formals.length() != argValues.length()) {
                    new BadArgumentCount(formals.length(), argValues.length()).signal();
                }
                yield ifFn.apply(body, env.extend(formals, argValues));
            }
            case VPrim(var arity, var proc) -> {
                if (arity != argValues.length()) {
                    new BadArgumentCount(arity, argValues.length()).signal();
                }
                Value result;
                try {
                    result = proc.apply(argValues);
                } catch (InterpretationException ie) {
                    yield ifPrimError.apply(ie);
                }
                yield ifPrimValue.apply(result);
            }
            default -> new ExpectedFn(this).signal();
        };
    }

    /** Produce a `display`able rendition of `this` Value. */
    default String toDisplayableString() {
        return switch (this) {
            case VNum(var n) -> (n == (long) n) ? "" + (long) n : "" + n;
            case VBool(var b) -> b ? "#t" : "#f";
            case VStr(var s) -> s;
            case VPrim ignored -> "#<VPrim>";
            case VCell c -> "#<VCell " + c.contents().toDisplayableString() + ">";
            case VFn f -> "#<VFn " + new Form(f.formals()) + " " + f.body().unparse() + ">";
            case VObj ignored -> "#<VObj>";
        };
    }
}
