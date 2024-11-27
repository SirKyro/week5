package umlang.ast;

import immutable.List;
import syntax.AtomSymbol;

/**
 * A Let binds a number of new variables before executing the body.
 * INVARIANT: length of names == length of inits.
 */
public record Let(List<AtomSymbol> names, List<Exp> inits, Exp body) implements Exp {}