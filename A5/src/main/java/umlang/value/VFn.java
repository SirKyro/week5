package umlang.value;

import immutable.List;
import syntax.AtomSymbol;
import umlang.Environment;
import umlang.ast.Exp;

/**
 * A VFn is an umlang-implemented function value.
 */
public record VFn(List<AtomSymbol> formals, Exp body, Environment<Value> env) implements Value {
    @Override
    public boolean isStructuralEquivalenceAppropriate() {
        return false;
    }
}