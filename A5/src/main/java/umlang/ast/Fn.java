package umlang.ast;

import immutable.List;
import syntax.AtomSymbol;

/**
 * A Fn is a function expression (like lambda in Racket).
 */
public record Fn(List<AtomSymbol> formals, Exp body) implements Exp {}