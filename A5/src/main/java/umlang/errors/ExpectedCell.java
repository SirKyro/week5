package umlang.errors;

import umlang.value.Value;

/** During evaluation, a VCell was required, but something else was supplied. */
public record ExpectedCell(Value actual) implements Error {}
