package umlang.value;

import syntax.AtomSymbol;
import umlang.Environment;
import umlang.errors.InterpretationException;
import umlang.errors.MethodNotFound;

/**
 * A VObj is an object with a collection of available methods.
 * Methods are searched left-to-right.
 */
public record VObj(Environment<VFn> methods) implements Value {
    public VFn lookup(AtomSymbol selector) throws InterpretationException {
        return methods.lookup(selector).valueOr(() ->
            new MethodNotFound(selector).signal());
    }

    @Override
    public boolean isStructuralEquivalenceAppropriate() {
        return false;
    }
}