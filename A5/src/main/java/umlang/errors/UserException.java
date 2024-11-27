package umlang.errors;

import umlang.value.Value;

/** User code signalled an exception value `exn` using Throw (catchable by TryCatch). */
public record UserException(Value exn) implements Error {}
