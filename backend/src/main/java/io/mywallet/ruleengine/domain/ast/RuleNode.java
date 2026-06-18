package io.mywallet.ruleengine.domain.ast;

/**
 * Root of the trading-rule AST. Every node is a plain, immutable data record - there is no
 * "eval this string" escape hatch anywhere in this hierarchy, and no reflection is used to
 * invoke arbitrary methods. A rule is data, evaluated by {@code RuleEvaluator} walking a
 * closed, sealed type hierarchy; the only way to add new capability is to add a new sealed
 * permit and a corresponding evaluator branch, both requiring a code change and review -
 * never something a stored expression string can do at runtime. This is what
 * "parser sécurisé basé sur un arbre d'expression" (never {@code eval()}) means in practice.
 */
public sealed interface RuleNode permits BooleanExpression, NumericExpression {
}
