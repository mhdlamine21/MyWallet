package io.mywallet.ruleengine.domain.parser;

import io.mywallet.ruleengine.domain.ast.BooleanExpression;
import io.mywallet.ruleengine.domain.ast.NumericExpression;
import io.mywallet.ruleengine.domain.exception.RuleParseException;
import io.mywallet.ruleengine.domain.lexer.RuleLexer;
import io.mywallet.ruleengine.domain.lexer.Token;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Recursive-descent parser: tokens in, {@link BooleanExpression} AST out. This is the
 * entirety of how a stored rule string becomes something the system acts on - there is no
 * step anywhere that hands the string (or any substring of it) to a scripting engine,
 * {@code eval()}, reflection-based method invocation, or a shell. Grammar (informal):
 *
 * <pre>
 * boolExpr   := orExpr
 * orExpr     := andExpr (OR andExpr)*
 * andExpr    := unary (AND unary)*
 * unary      := NOT unary | primary
 * primary    := '(' boolExpr ')' | comparison
 * comparison := numeric compOp numeric
 * compOp     := '>' | '<' | '==' | CROSSES_ABOVE | CROSSES_BELOW
 * numeric    := NUMBER ['%'] | IDENTIFIER '(' IDENTIFIER ',' NUMBER ')' | IDENTIFIER
 * </pre>
 */
public final class RuleParser {

    private static final Set<String> KNOWN_PORTFOLIO_METRICS = Set.of(
        "PORTFOLIOEXPOSURE", "DAILYLOSS", "CONSECUTIVELOSSES"
    );

    private final List<Token> tokens;
    private int position = 0;

    private RuleParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /** Parses a full rule expression string into a {@link BooleanExpression} AST. */
    public static BooleanExpression parse(String expression) {
        List<Token> tokens = new RuleLexer(expression).tokenize();
        RuleParser parser = new RuleParser(tokens);
        BooleanExpression result = parser.parseBoolExpr();
        parser.expect(Token.TokenType.EOF, "end of expression");
        return result;
    }

    private BooleanExpression parseBoolExpr() {
        return parseOr();
    }

    private BooleanExpression parseOr() {
        BooleanExpression left = parseAnd();
        while (check(Token.TokenType.OR)) {
            advance();
            BooleanExpression right = parseAnd();
            left = new BooleanExpression.LogicalOr(left, right);
        }
        return left;
    }

    private BooleanExpression parseAnd() {
        BooleanExpression left = parseUnary();
        while (check(Token.TokenType.AND)) {
            advance();
            BooleanExpression right = parseUnary();
            left = new BooleanExpression.LogicalAnd(left, right);
        }
        return left;
    }

    private BooleanExpression parseUnary() {
        if (check(Token.TokenType.NOT)) {
            advance();
            return new BooleanExpression.LogicalNot(parseUnary());
        }
        return parsePrimary();
    }

    private BooleanExpression parsePrimary() {
        if (check(Token.TokenType.LPAREN)) {
            advance();
            BooleanExpression inner = parseBoolExpr();
            expect(Token.TokenType.RPAREN, ")");
            return inner;
        }
        return parseComparison();
    }

    private BooleanExpression parseComparison() {
        NumericExpression left = parseNumeric();
        BooleanExpression.ComparisonOperator operator = parseComparisonOperator();
        NumericExpression right = parseNumeric();
        return new BooleanExpression.Comparison(left, operator, right);
    }

    private BooleanExpression.ComparisonOperator parseComparisonOperator() {
        Token token = peek();
        BooleanExpression.ComparisonOperator op = switch (token.type()) {
            case GREATER_THAN -> BooleanExpression.ComparisonOperator.GREATER_THAN;
            case LESS_THAN -> BooleanExpression.ComparisonOperator.LESS_THAN;
            case EQUALS -> BooleanExpression.ComparisonOperator.EQUALS;
            case CROSSES_ABOVE -> BooleanExpression.ComparisonOperator.CROSSES_ABOVE;
            case CROSSES_BELOW -> BooleanExpression.ComparisonOperator.CROSSES_BELOW;
            default -> throw new RuleParseException(
                "Expected a comparison operator (>, <, ==, CROSSES_ABOVE, CROSSES_BELOW) but found '%s'".formatted(token.text()));
        };
        advance();
        return op;
    }

    private NumericExpression parseNumeric() {
        Token token = peek();

        if (token.type() == Token.TokenType.NUMBER) {
            advance();
            BigDecimal value = new BigDecimal(token.text());
            if (check(Token.TokenType.PERCENT)) {
                advance();
                value = value.divide(BigDecimal.valueOf(100));
            }
            return new NumericExpression.Literal(value);
        }

        if (token.type() == Token.TokenType.IDENTIFIER) {
            advance();
            if (check(Token.TokenType.LPAREN)) {
                return parseIndicatorCall(token.text());
            }
            return parsePortfolioMetric(token.text());
        }

        throw new RuleParseException("Expected a number, indicator call, or portfolio metric but found '%s'".formatted(token.text()));
    }

    private NumericExpression parseIndicatorCall(String name) {
        expect(Token.TokenType.LPAREN, "(");
        Token symbolToken = expect(Token.TokenType.IDENTIFIER, "an asset symbol");
        expect(Token.TokenType.COMMA, ",");
        Token periodToken = expect(Token.TokenType.NUMBER, "a period (number)");
        expect(Token.TokenType.RPAREN, ")");

        NumericExpression.IndicatorType type = resolveIndicatorType(name);
        int period = parsePositiveInt(periodToken.text(), name);
        return new NumericExpression.IndicatorCall(type, symbolToken.text(), period);
    }

    private NumericExpression.IndicatorType resolveIndicatorType(String name) {
        try {
            return NumericExpression.IndicatorType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException notSupported) {
            throw new RuleParseException(
                "Unknown or not-yet-supported indicator '%s'. Supported in this phase: %s"
                    .formatted(name, List.of(NumericExpression.IndicatorType.values())));
        }
    }

    private NumericExpression parsePortfolioMetric(String name) {
        String normalized = name.toUpperCase();
        if (!KNOWN_PORTFOLIO_METRICS.contains(normalized)) {
            throw new RuleParseException(
                "Unknown identifier '%s' - expected an indicator call like SMA(SYMBOL, period) or a portfolio metric like PortfolioExposure".formatted(name));
        }
        NumericExpression.PortfolioMetricType type = switch (normalized) {
            case "PORTFOLIOEXPOSURE" -> NumericExpression.PortfolioMetricType.EXPOSURE;
            case "DAILYLOSS" -> NumericExpression.PortfolioMetricType.DAILY_LOSS;
            case "CONSECUTIVELOSSES" -> NumericExpression.PortfolioMetricType.CONSECUTIVE_LOSSES;
            default -> throw new IllegalStateException("Unreachable: " + normalized);
        };
        return new NumericExpression.PortfolioMetric(type);
    }

    private int parsePositiveInt(String text, String context) {
        try {
            int value = Integer.parseInt(text);
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new RuleParseException("Expected a positive integer period for %s(...), got '%s'".formatted(context, text));
        }
    }

    // token stream helpers

    private Token peek() {
        return tokens.get(position);
    }

    private boolean check(Token.TokenType type) {
        return peek().type() == type;
    }

    private void advance() {
        if (position < tokens.size() - 1) {
            position++;
        }
    }

    private Token expect(Token.TokenType type, String expectedDescription) {
        Token token = peek();
        if (token.type() != type) {
            throw new RuleParseException("Expected %s but found '%s'".formatted(expectedDescription, token.text()));
        }
        advance();
        return token;
    }
}
