package immutable;

/** Represents an absent X in a Maybe<X>. */
public record None<X>() implements Maybe<X> {}