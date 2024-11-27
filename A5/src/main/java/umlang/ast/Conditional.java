package umlang.ast;

/**
 * A Conditional is an if-then-else expression.
 */
public record Conditional(Exp test, Exp ifTrue, Exp ifFalse) implements Exp {}