package umlang.ast;

import immutable.List;
import syntax.AtomSymbol;

/**
 * An ExtendObj expression extends `base` with a new method responding to `selector`.
 */
public record ExtendObj(AtomSymbol selector, List<AtomSymbol> formals, Exp body, Exp base) implements Exp {}