package umlang.ast;

import syntax.AtomSymbol;

/** A Definition is a ToplevelClause that introduces a new global variable. */
public record Definition(AtomSymbol name, Exp initializer) implements ToplevelClause {}