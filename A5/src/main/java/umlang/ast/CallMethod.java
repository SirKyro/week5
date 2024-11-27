package umlang.ast;

import immutable.List;
import syntax.AtomSymbol;

/**
 * A CallMethod is a method call expression, invoking `selector` on `objExp` with `args`
 */
public record CallMethod(Exp objExp, AtomSymbol selector, List<Exp> args) implements Exp {}