package umlang.vm;

import immutable.Cons;
import immutable.List;
import immutable.Nil;
import immutable.ThrowingBiFunction;
import syntax.AtomSymbol;
import umlang.Environment;
import umlang.Interpreter;
import umlang.ast.Bool;
import umlang.ast.Call;
import umlang.ast.CallMethod;
import umlang.ast.Conditional;
import umlang.ast.Exp;
import umlang.ast.ExtendObj;
import umlang.ast.Fn;
import umlang.ast.InertObj;
import umlang.ast.Let;
import umlang.ast.Num;
import umlang.ast.Ref;
import umlang.ast.Seq;
import umlang.ast.Str;
import umlang.ast.Throw;
import umlang.ast.TryCatch;
import umlang.errors.InterpretationException;
import umlang.errors.UserException;
import umlang.value.VBool;
import umlang.value.VFn;
import umlang.value.VNum;
import umlang.value.VObj;
import umlang.value.VStr;
import umlang.value.Value;

/** A simple virtual-machine style interpreter for Exps. */
public class VM extends Interpreter {
    /**
     * A MachineState is either an EvalState or an ApplyState.
     * It represents the complete state of a computation mid-execution.
    */
    private sealed interface MachineState permits EvalState, ApplyState {}
    /**
     * An EvalState is a machine about to evaluate `exp` in environment `env`.
     * When `exp` is completely evaluated to some value, the value will be passed on to
     * the first frame in `continuation`. If no continuations remain, the whole machine terminates
     * and the value is the result.
     */
    private record EvalState(Exp exp, Environment<Value> env, List<Frame> continuation) implements MachineState {}
    /**
     * An ApplyState is a machine about to return `value` to the first frame in `continuation` (see EvalState).
     */
    private record ApplyState(Value value, List<Frame> continuation) implements MachineState {}

    /**
     * A Frame is a fragment of a suspended computation. It represents the "next thing to do" to drive the
     * overall computation forward. Some kinds of `Exp` have their own kinds of Frame.
     * Whenever an ApplyState is processed, the value it contains is given to the top Frame; each kind of frame
     * handles the value in its own way.
     */
    private sealed interface Frame permits IfFrame, EvalListFrame, ExtendObjFrame, FnValFrame, ObjValFrame, SeqFrame, ThrowFrame, CatchFrame {
        /** Convenience method for prepending a Frame to a continuation. */
        default List<Frame> then(List<Frame> subsequentFrames) {
            return new Cons<>(this, subsequentFrames);
        }
    }
    /**
     * An IfFrame is waiting for the "test" in a conditional to yield a value. When the value arrives,
     * one of `ifTrue` or `ifFalse` will be chosen to continue executing in `env`.
     */
    private record IfFrame(Environment<Value> env, Exp ifTrue, Exp ifFalse) implements Frame {}
    /**
     * An EvalListFrame is part-way through evaluating e.g. an argument list.
     * When given a value, it is prepended to doneRev. Then, if rest is empty, whenComplete is called.
     * Otherwise, another EvalListFrame is prepended to the continuation and the machine switches
     * to evaluating the first of rest.
     */
    private record EvalListFrame(
        List<Value> doneRev,
        ThrowingBiFunction<List<Value>, List<Frame>, MachineState, InterpretationException> whenComplete,
        Environment<Value> env,
        List<Exp> rest
    ) implements Frame {}
    /**
     * An ExtendObjFrame is waiting for an object value to extend with `selector` and `method`.
     * It then yields the resulting extended object to the next frame in the continuation.
     */
    private record ExtendObjFrame(VFn method, AtomSymbol selector) implements Frame {}
    /**
     * A FnValFrame is waiting for a function value to call. When it arrives, it uses EvalListFrame
     * to evaluate `args` in `env`, supplying a whenComplete that transfers control to the function's body.
     */
    private record FnValFrame(List<Exp> args, Environment<Value> env) implements Frame {}
    /**
     * An ObjValFrame is like a FnValFrame, but for a pending method call.
     */
    private record ObjValFrame(Environment<Value> env, AtomSymbol selector, List<Exp> args) implements Frame {}
    /**
     * A SeqFrame discards the value it is given. Then, if only one Exp remains in `more`, it
     * switches to evaluating that. Otherwise, it pushes another SeqFrame with the rest of `more` and then
     * switches to the first of `more`.
     */
    private record SeqFrame(Cons<Exp> more, Environment<Value> env) implements Frame {}
    /**
     * A ThrowFrame represents a pending `throw`, waiting for the exception value to throw.
     */
    private record ThrowFrame() implements Frame {}
    /**
     * A CatchFrame represents an active exception-catching frame. When Throw is evaluated,
     * the continuation is unwound until a CatchFrame is found or the continuation is empty.
     */
    private record CatchFrame(AtomSymbol exnVar, Exp handler, Environment<Value> env) implements Frame {}

    /**
     * Evaluate `initialExp` in `initialEnv` and return the resulting Value.
     */
    @Override
    public Value evaluate(Exp initialExp, Environment<Value> initialEnv) throws InterpretationException {
        return execute(new EvalState(initialExp, initialEnv, List.empty()));
    }

    /* Execute from the given `state`. */
    private Value execute(MachineState state) throws InterpretationException {
        while (true) {
            if (noisy) System.err.println(state);
            switch (state) {
                case ApplyState(var val, Nil<Frame> ignored) -> {
                    return val;
                }
                case ApplyState(var val, Cons<Frame>(var frame, var continuation)) -> state = switch (frame) {
                    case IfFrame(var env, var ifTrue, var ifFalse) ->
                        new EvalState(val.isTruthy() ? ifTrue : ifFalse, env, continuation);
                    case EvalListFrame(var doneRev, var whenComplete, var env, var rest) ->
                        evalList(rest, env, continuation, new Cons<>(val, doneRev), whenComplete);
                    case ExtendObjFrame(var method, var selector) ->
                        new ApplyState(new VObj(val.asObj().methods().extend(selector, method)), continuation);
                    case FnValFrame(var args, var env) ->
                        evalList(args, env, continuation, List.empty(),
                            (argVals, k) -> invoke(val, argVals, k));
                    case ObjValFrame(var env, var selector, var args) ->
                        evalList(args, env, continuation, List.empty(),
                            (argVals, k) -> invoke(val.asObj().lookup(selector), new Cons<>(val, argVals), k));
                    case SeqFrame(var more, var env) ->
                        new EvalState(new Seq(more), env, continuation);
                    case ThrowFrame() ->
                        throw new RuntimeException("UNIMPLEMENTED ThrowFrame in umlang.vm.VM");
                    case CatchFrame(var exnVar, var handler, var env) ->
                        throw new RuntimeException("UNIMPLEMENTED CatchFrame in umlang.vm.VM");
                };
                case EvalState(var exp, var env, var continuation) -> state = switch (exp) {
                    case Ref(var name) ->
                        new ApplyState(lookup(name, env), continuation);
                    case Num(var n) ->
                        new ApplyState(new VNum(n), continuation);
                    case Bool(var b) ->
                        new ApplyState(new VBool(b), continuation);
                    case Str(var s) ->
                        new ApplyState(new VStr(s), continuation);
                    case Conditional(var test, var ifTrue, var ifFalse) ->
                        new EvalState(test, env, new IfFrame(env, ifTrue, ifFalse).then(continuation));
                    case Let(var names, var inits, var body) ->
                        evalList(inits, env, continuation, List.empty(),
                            (values, k1) -> new EvalState(body, env.extend(names, values), k1));
                    case Fn(var formals, var body) ->
                        new ApplyState(new VFn(formals, body, env), continuation);
                    case Call(var fnExp, var args) ->
                        new EvalState(fnExp, env, new FnValFrame(args, env).then(continuation));
                    case Seq(var exps) ->
                        new EvalState(exps.first(), env, exps.rest() instanceof Cons<Exp> more
                            ? new SeqFrame(more, env).then(continuation)
                            : continuation);
                    case InertObj() ->
                        new ApplyState(new VObj(new Environment<>(List.empty())), continuation);
                    case ExtendObj(var selector, var formals, var body, var base) -> {
                        var method = new VFn(new Cons<>(new AtomSymbol("self"), formals), body, env);
                        yield new EvalState(base, env, new ExtendObjFrame(method, selector).then(continuation));
                    }
                    case CallMethod(var objExp, var selector, var args) ->
                        new EvalState(objExp, env, new ObjValFrame(env, selector, args).then(continuation));
                    case Throw(var exnExp) ->
                        throw new RuntimeException("UNIMPLEMENTED Throw in umlang.vm.VM");
                    case TryCatch(var body, var exnVar, var handler) ->
                        throw new RuntimeException("UNIMPLEMENTED TryCatch in umlang.vm.VM");
                };
            }
        }
    }

    private static MachineState invoke(Value fnVal, List<Value> argValues, List<Frame> continuation) throws InterpretationException {
        return fnVal.invoke(
            argValues,
            (body, extended) -> new EvalState(body, extended, continuation),
            (value) -> new ApplyState(value, continuation),
            (exn) -> {
                if (exn.error() instanceof UserException(var ue)) {
                    throw new RuntimeException("UNIMPLEMENTED handling UserException from primitive in umlang.vm.VM");
                } else {
                    throw exn;
                }
            });
    }

    @Override
    public Value callMethod(Value receiver, AtomSymbol selector, List<Value> arguments) throws InterpretationException {
        return execute(invoke(receiver.asObj().lookup(selector), new Cons<>(receiver, arguments), List.empty()));
    }

    private static MachineState evalList(
        List<Exp> remaining,
        Environment<Value> env,
        List<Frame> continuation,
        List<Value> doneRev,
        ThrowingBiFunction<List<Value>, List<Frame>, MachineState, InterpretationException> whenComplete
    ) throws InterpretationException {
        return switch (remaining) {
            case Nil<Exp> ignored -> whenComplete.apply(doneRev.reverse(), continuation);
            case Cons(var first, var rest) -> new EvalState(first, env, new EvalListFrame(doneRev, whenComplete, env, rest).then(continuation));
        };
    }

    // True if each machine state should be printed. Crude debugging/monitoring.
    private static final boolean noisy;
    static {
        var envvar = System.getenv("UMLANG_VM_NOISY");
        noisy = envvar != null && envvar.equals("1");
    }
}
