package umlang;

import syntax.AtomSymbol;

/**
 * An Entry<X> is an entry in an Environment<X>.
 * Each Entry represents a name-value pair, where the value is of type X.
 */
public record Entry<X>(AtomSymbol name, X value) {}