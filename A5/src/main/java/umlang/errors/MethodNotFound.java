package umlang.errors;

import syntax.AtomSymbol;

/** Method `selector` was not found during method call. */
public record MethodNotFound(AtomSymbol name) implements Error {}
