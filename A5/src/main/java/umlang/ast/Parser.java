package umlang.ast;

import immutable.Cons;
import immutable.List;
import immutable.Nil;
import immutable.Some;
import syntax.AtomNumber;
import syntax.AtomString;
import syntax.AtomSymbol;
import syntax.Form;
import syntax.Term;

/**
 * Parser only exists to hold the static method `parse`.
 */
public class Parser {
    /** Parse an S-expression into a ToplevelClause if possible. Throws ParseError if not. */
    public static ToplevelClause parseToplevel(Term term) {
        if (term instanceof Form(Cons(AtomSymbol kwDefine, var rest)) && kwDefine.label().equals("define")) {
            if (rest instanceof Cons(AtomSymbol name, Cons(Term initializer, Nil()))) {
                return new Definition(name, parse(initializer));
            } else {
                return new ParseError("Bad 'define' syntax", term).signal();
            }
        } else {
            return parse(term);
        }
    }

    /** Parse an S-expression into an Exp if possible. Throws ParseError if not. */
    public static Exp parse(Term term) {
        return switch (term) {
            case AtomNumber(var n) -> new Num(n);
            case AtomString(var s) -> new Str(s);
            case AtomSymbol(var label) -> switch (label) {
                case "#t" -> new Bool(true);
                case "#f" -> new Bool(false);
                default -> new Ref(new AtomSymbol(label));
            };
            case Form(Cons(AtomSymbol(var firstSym), List<? extends Term> rest)) -> switch (firstSym) {
                case "if" -> parseIf(term, rest);
                case "let" -> parseLet(term, rest);
                case "fn" -> parseFn(term, rest);
                case "seq" -> rest instanceof Cons<? extends Term> c
                    ? new Seq(c.map(Parser::parse))
                    : new ParseError("Bad 'seq' syntax", term).signal();
                case "obj" -> parseObj(term, rest);
                case "throw" -> parseThrow(term, rest);
                case "catch" -> parseCatch(term, rest);
                default -> parseCallOrCallMethod(term, term.form().unwrap());
            };
            case Form(var terms) -> parseCallOrCallMethod(term, terms);
        };
    }

    /** Parse a call or a method-call */
    private static Exp parseCallOrCallMethod(Term term, List<? extends Term> terms) {
        if (terms.length() >= 2 && terms.get(1).symbol().map(Parser::isSelector).valueOr(false)) {
            return new CallMethod(
                parse(terms.get(0)),
                terms.get(1).symbol().unwrap(),
                terms.cons().unwrap().rest().cons().unwrap().rest().map(Parser::parse));
        } else if (terms instanceof Cons(var first, var rest)) {
            return new Call(parse(first), rest.map(Parser::parse));
        } else {
            return new ParseError("Parse error", term).signal();
        }
    }

    /** Parse an `if` expression */
    private static Conditional parseIf(Term term, List<? extends Term> rest) {
        if (rest.length() == 3) {
            return new Conditional(parse(rest.get(0)), parse(rest.get(1)), parse(rest.get(2)));
        }
        return new ParseError("Bad 'if' syntax", term).signal();
    }

    /** Parse a `let` expression */
    private static Let parseLet(Term term, List<? extends Term> rest) {
        if (rest.length() == 2 && rest.get(0).form() instanceof Some(var namesAndInits)) {
            if (namesAndInits.every((ni) -> ni.form().map((f) -> f.length() == 2 && f.get(0) instanceof AtomSymbol).valueOr(false))) {
                var names = namesAndInits.map((ni) -> ni.form().unwrap().get(0).symbol().unwrap());
                var inits = namesAndInits.map((ni) -> parse(ni.form().unwrap().get(1)));
                return new Let(names, inits, parse(rest.get(1)));
            }
        }
        return new ParseError("Bad 'let' syntax", term).signal();
    }

    /** Parse a `fn` expression */
    private static Fn parseFn(Term term, List<? extends Term> rest) {
        if (rest.length() == 2 && rest.get(0).form().map((fs) -> fs.every((f) -> f instanceof AtomSymbol)).valueOr(false)) {
            return new Fn(rest.get(0).form().unwrap().map((f) -> f.symbol().unwrap()), parse(rest.get(1)));
        }
        return new ParseError("Bad 'fn' syntax", term).signal();
    }

    /** Parse an `obj` expression */
    private static Exp parseObj(Term term, List<? extends Term> rest) {
        List<? extends Term> remaining = rest.reverse();
        Exp base = new InertObj();
        if (remaining.length() >= 2 && remaining.get(1).symbol().map((s) -> s.label().equals("#:base")).valueOr(false)) {
            base = parse(remaining.get(0));
            remaining = remaining.cons().unwrap().rest().cons().unwrap().rest();
        }
        while (remaining instanceof Cons(var method, var more)) {
            if (method.form().map(Parser::isValidMethodDefinition).valueOr(false)) {
                var m = method.form().unwrap();
                base = new ExtendObj(
                    m.get(0).symbol().unwrap(),
                    m.get(1).form().unwrap().map((formal) -> formal.symbol().unwrap()),
                    parse(m.get(2)),
                    base);
                remaining = more;
            } else {
                new ParseError("Bad 'obj' syntax", term).signal();
            }
        }
        return base;
    }

    private static boolean isSelector(AtomSymbol s) {
        return s.label().length() > 0 && s.label().charAt(0) == ':';
    }

    private static boolean isValidMethodDefinition(List<? extends Term> f) {
        return f.length() == 3
            && f.get(0).symbol().map(Parser::isSelector).valueOr(false)
            && f.get(1).form().map((formals) -> formals.every(
                (formal) -> formal.symbol().isSome())).valueOr(false);
    }

    /** Parse a `throw` expression */
    private static Exp parseThrow(Term term, List<? extends Term> rest) {
        if (rest.length() == 1) {
            return new Throw(parse(rest.get(0)));
        } else {
            return new ParseError("Bad 'throw' syntax", term).signal();
        }
    }

    /** Parse a `catch` expression */
    private static Exp parseCatch(Term term, List<? extends Term> rest) {
        if (rest instanceof Cons(Term exnStx, Cons(Form(Cons(AtomSymbol s, Nil())), Cons(Term handlerStx, Nil())))) {
            return new TryCatch(parse(exnStx), s, parse(handlerStx));
        } else {
            return new ParseError("Bad 'catch' syntax", term).signal();
        }
    }
}
