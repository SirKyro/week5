package umlang;

import immutable.Cons;
import immutable.List;
import syntax.AtomSymbol;
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
import umlang.value.VBool;
import umlang.value.VFn;
import umlang.value.VNum;
import umlang.value.VObj;
import umlang.value.VStr;
import umlang.value.Value;

public class RecursiveInterpreter extends Interpreter {
    @Override
    public Value evaluate(Exp exp, Environment<Value> env) throws InterpretationException {
        return switch (exp) {
            case Ref(var name) -> lookup(name, env);
            case Num(var n) -> new VNum(n);
            case Bool(var b) -> new VBool(b);
            case Str(var s) -> new VStr(s);
            case Conditional(var test, var ifTrue, var ifFalse) ->
                evaluate(evaluate(test, env).isTruthy() ? ifTrue : ifFalse, env);
            case Let(var names, var inits, var body) ->
                evaluate(body, env.extend(names, inits.map((i) -> evaluate(i, env))));
            case Fn(var formals, var body) -> new VFn(formals, body, env);
            case Call(var fnExp, var args) ->
                invoke(evaluate(fnExp, env), args.map((i) -> evaluate(i, env)));
            case Seq(var exps) -> {
                while (exps.rest() instanceof Cons<Exp> more) {
                    evaluate(exps.first(), env);
                    exps = more;
                }
                yield evaluate(exps.first(), env);
            }
            case InertObj() -> new VObj(new Environment<VFn>(List.empty()));
            case ExtendObj(var selector, var formals, var body, var base) -> {
                var baseObj = evaluate(base, env).asObj();
                var method = new VFn(new Cons<AtomSymbol>(new AtomSymbol("self"), formals), body, env);
                yield new VObj(baseObj.methods().extend(selector, method));
            }
            case CallMethod(var objExp, var selector, var args) ->
                callMethod(evaluate(objExp, env), selector, args.map((i) -> evaluate(i, env)));
            case Throw(var exnExp) ->
                throw new RuntimeException("UNIMPLEMENTED Throw in RecursiveInterpreter");
            case TryCatch(var body, var exnVar, var handler) ->
                throw new RuntimeException("UNIMPLEMENTED TryCatch in RecursiveInterpreter");
        };
    }

    /** Invokes `callable` as a function with `argValues`. */
    private Value invoke(Value callable, List<Value> argValues) throws InterpretationException {
        return callable.invoke(argValues, this::evaluate, (result) -> result, (exn) -> { throw exn; });
    }

    @Override
    public Value callMethod(Value receiver, AtomSymbol selector, List<Value> arguments) throws InterpretationException {
        return invoke(receiver.asObj().lookup(selector), new Cons<>(receiver, arguments));
    }
}