package umlang.ast;

/**
 * A Bool is a literal boolean expression.
 */
public record Bool(boolean value) implements Exp {}