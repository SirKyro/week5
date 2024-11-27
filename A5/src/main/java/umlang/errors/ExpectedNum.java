package umlang.errors;

import umlang.value.Value;

/** During evaluation, a VNum was required, but something else was supplied. */
public record ExpectedNum(Value actual) implements Error {}
