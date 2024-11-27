package umlang.ast;

import syntax.AtomSymbol;

/**
 * A TryCatch expression evaluates `body`. If `body` throws an exception using a Throw expression, `handler` is invoked with `exnVar` bound to the exception value.
 */
public record TryCatch(Exp body, AtomSymbol exnVar, Exp handler) implements Exp {}