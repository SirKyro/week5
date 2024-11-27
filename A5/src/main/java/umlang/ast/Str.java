package umlang.ast;

/**
 * A Str is a literal string expression.
 */
public record Str(String value) implements Exp {}