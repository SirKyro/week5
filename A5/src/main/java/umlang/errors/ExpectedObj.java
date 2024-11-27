package umlang.errors;

import umlang.value.Value;

/** During evaluation, a VObj was required, but something else was supplied. */
public record ExpectedObj(Value actual) implements Error {}
