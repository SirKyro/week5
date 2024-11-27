package umlang.ast;

import immutable.List;

/**
 * A Call is a function call expression.
 */
public record Call(Exp fnExp, List<Exp> args) implements Exp {}