package syntax;

/** An Atom represents an atomic S-expression: a symbol, number or string. */
public sealed interface Atom extends Term permits AtomSymbol, AtomNumber, AtomString {}
