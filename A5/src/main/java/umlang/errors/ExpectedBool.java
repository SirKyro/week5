package umlang.errors;

import umlang.value.Value;

/** During evaluation, a VBool was required, but something else was supplied. */
public record ExpectedBool(Value actual) implements Error {}
