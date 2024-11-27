package umlang.ast;

import syntax.AtomSymbol;

/**
 * A Ref is a variable reference.
 */
public record Ref(AtomSymbol name) implements Exp {}