package umlang.ast;

/**
 * A Num is a literal number expression.
 */
public record Num(double value) implements Exp {}