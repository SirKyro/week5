package umlang.errors;

import syntax.AtomSymbol;

/** Variable `name` declared, but not initialized, at the point the error was signalled. */
public record UninitializedGlobal(AtomSymbol name) implements Error {}
