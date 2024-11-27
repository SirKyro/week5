package umlang.ast;

import immutable.Cons;
import immutable.List;
import syntax.AtomNumber;
import syntax.AtomString;
import syntax.AtomSymbol;
import syntax.Form;
import syntax.Term;

/**
 * An Exp is an expression in UMlang.
 */
public sealed interface Exp extends ToplevelClause permits
    Ref,
    Num,
    Bool,
    Str,
    Conditional,
    Let,
    Fn,
    Call,
    Seq,
    InertObj,
    ExtendObj,
    CallMethod,
    Throw,
    TryCatch
{
    /**
     * Compute an approximation to the concrete syntax corresponding to `this`.
     * Not intended for round-tripping or re-compilation: just for debugging and
     * helping the programmer understand the meaning of an `Exp`.
     */
    default Term unparseHelper(){
        return switch (this){
            case Ref(var name) -> name;
            case Num(var n) -> new AtomNumber(n);
            case Bool(var b) -> new AtomSymbol(b ? "#t" : "#f");
            case Str(var s) -> new AtomString(s);
            case Conditional(var test, var ifTrue, var ifFalse) ->
                new Form(new AtomSymbol("if"), test.unparse(), ifTrue.unparse(), ifFalse.unparse());
            case Let(var names, var inits, var body) ->
                new Form(
                    new AtomSymbol("let"),
                    new Form(List.map((n, i) -> new Form(n, i.unparse()), names, inits)),
                    body.unparse());
            case Fn(var formals, var body) ->
                new Form(new AtomSymbol("fn"), new Form(formals), body.unparse());
            case Call(var fnExp, var args) ->
                new Form(new Cons<>(fnExp.unparse(), args.<Term, RuntimeException>map(Exp::unparse)));
            case Seq(var exps) ->
                new Form(new Cons<>(new AtomSymbol("seq"), exps.<Term, RuntimeException>map(Exp::unparse)));
            case InertObj() -> new Form(new AtomSymbol("obj"));
            case ExtendObj(var selector, var formals, var body, var base) -> 
                new Form(
                        //new AtomSymbol("obj"),
                        new Form(selector, new Form(formals), body.unparse()),
                        new AtomSymbol("#:base"),
                        base.unparseHelper()
                    );
            
            case CallMethod(var objExp, var selector, var args) ->
                new Form(new Cons<>(objExp.unparse(), new Cons<>(selector, args.map(Exp::unparse))));
            case Throw(var exnExp) -> new Form(new AtomSymbol("throw"), exnExp.unparse());
            case TryCatch(var body, var exnVar, var handler) -> new Form(new AtomSymbol("catch"), body.unparse(), new Form(exnVar), handler.unparse());
        };
        }
    
    
    default Term unparse() {
        return switch (this) {
            case Ref(var name) -> name;
            case Num(var n) -> new AtomNumber(n);
            case Bool(var b) -> new AtomSymbol(b ? "#t" : "#f");
            case Str(var s) -> new AtomString(s);
            case Conditional(var test, var ifTrue, var ifFalse) ->
                new Form(new AtomSymbol("if"), test.unparse(), ifTrue.unparse(), ifFalse.unparse());
            case Let(var names, var inits, var body) ->
                new Form(
                    new AtomSymbol("let"),
                    new Form(List.map((n, i) -> new Form(n, i.unparse()), names, inits)),
                    body.unparse());
            case Fn(var formals, var body) ->
                new Form(new AtomSymbol("fn"), new Form(formals), body.unparse());
            case Call(var fnExp, var args) ->
                new Form(new Cons<>(fnExp.unparse(), args.<Term, RuntimeException>map(Exp::unparse)));
            case Seq(var exps) ->
                new Form(new Cons<>(new AtomSymbol("seq"), exps.<Term, RuntimeException>map(Exp::unparse)));
            case InertObj() -> new Form(new AtomSymbol("obj"));
            case ExtendObj(var selector, var formals, var body, var base) -> 
                new Form(
                        new AtomSymbol("obj"),
                        new Form(selector, new Form(formals), body.unparse()),
                        new AtomSymbol("#:base"),
                        base.unparseHelper()
                    );
            
            case CallMethod(var objExp, var selector, var args) ->
                new Form(new Cons<>(objExp.unparse(), new Cons<>(selector, args.map(Exp::unparse))));
            case Throw(var exnExp) -> new Form(new AtomSymbol("throw"), exnExp.unparse());
            case TryCatch(var body, var exnVar, var handler) -> new Form(new AtomSymbol("catch"), body.unparse(), new Form(exnVar), handler.unparse());
        };
    }
}
