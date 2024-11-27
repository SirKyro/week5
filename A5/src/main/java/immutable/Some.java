package immutable;

/** Represents an present X in a Maybe<X>. */
public record Some<X>(X value) implements Maybe<X> {}