package umlang.ast;

import immutable.Cons;

/**
 * A Seq is a sequencing expression: `exps` are evaluated in order, and
 * the last one is the result of the Seq.
 *
 * (Notice that using type `Cons<Exp>` works just like `NonEmptyList<Exp>`!)
 */
public record Seq(Cons<Exp> exps) implements Exp {}