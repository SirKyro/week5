package umlang.errors;

/** Expected `expected` arguments, but got `actual`. */
public record BadArgumentCount(int expected, int actual) implements Error {}
