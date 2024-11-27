package umlang.ast;

/** A ToplevelClause is either an Exp (evaluated for its value or side-effect) or a Definition (evaluated to extend the global environment) */
public sealed interface ToplevelClause permits Exp, Definition {}