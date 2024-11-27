package umlang.errors;

import umlang.value.Value;

/** During evaluation, a VFn was required, but something else was supplied. */
public record ExpectedFn(Value actual) implements Error {}
