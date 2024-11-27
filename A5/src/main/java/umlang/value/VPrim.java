package umlang.value;

import immutable.List;
import immutable.ThrowingFunction;
import umlang.errors.InterpretationException;

/**
 * A VPrim is a function value backed by a Java-language method.
 * The `arity` is the number of arguments the primitive expects.
 */
public record VPrim(int arity, ThrowingFunction<List<Value>, Value, InterpretationException> proc) implements Value {}