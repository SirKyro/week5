package umlang;

import immutable.Cons;
import immutable.List;
import immutable.Maybe;
import syntax.AtomSymbol;

/**
 * An Environment<X> is a ListOf<Entry<X>>.
 * Each Entry represents a name-value pair, where the value is of type X.
 */
public record Environment<X>(List<Entry<X>> entries) {
    /** Convenience constructor for empty environments. */
    public Environment() {
        this(List.empty());
    }

    /** Answer `this`, but extended with the given names and values. */
    public Environment<X> extend(List<AtomSymbol> names, List<X> values) {
        return new Environment<>(List.fold(
            this.entries,
            (n, v, es) -> new Cons<>(new Entry<>(n, v), es),
            names,
            values));
    }

    /** Answer `this`, but extended with the given name and value. */
    public Environment<X> extend(AtomSymbol name, X value) {
        return new Environment<>(new Cons<>(new Entry<>(name, value), entries));
    }

    /** Retrieve a value matching `name`, if any. */
    public Maybe<X> lookup(AtomSymbol name) {
        return entries.find((e) -> e.name().equals(name)).map(Entry::value);
    }
}
