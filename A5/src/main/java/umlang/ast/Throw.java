package umlang.ast;

/**
 * A Throw expression computes an exception value from `exnExp` and then throws it up the stack to a waiting TryCatch expression, if any exists.
 */
public record Throw(Exp exnExp) implements Exp {}