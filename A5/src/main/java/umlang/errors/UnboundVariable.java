package umlang.errors;

import syntax.AtomSymbol;

/** Variable `name` was not in scope during evaluation. */
public record UnboundVariable(AtomSymbol name) implements Error {}
