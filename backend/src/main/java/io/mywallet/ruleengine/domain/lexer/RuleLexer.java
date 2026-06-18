package io.mywallet.ruleengine.domain.lexer;

import io.mywallet.ruleengine.domain.exception.RuleParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Tokenizes a rule expression string into a flat list of {@link Token}s. Purely
 * character-by-character scanning - recognizes a fixed, closed set of symbols and
 * keywords (see {@link Token.TokenType}); anything outside that set is a
 * {@link RuleParseException}, never silently passed through. This is what makes it safe
 * to feed user-authored strategy text into: the lexer has no notion of "unknown token,
 * try to execute it as code" - unknown input is always a hard parse error.
 */
public final class RuleLexer {

    private static final Set<String> KEYWORDS = Set.of(
        "AND", "OR", "NOT", "CROSSES_ABOVE", "CROSSES_BELOW"
    );

    private final String source;
    private int position = 0;

    public RuleLexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = nextToken();
            tokens.add(token);
        } while (token.type() != Token.TokenType.EOF);
        return tokens;
    }

    private Token nextToken() {
        skipWhitespace();
        if (position >= source.length()) {
            return new Token(Token.TokenType.EOF, "");
        }

        char c = source.charAt(position);

        if (Character.isLetter(c) || c == '_') {
            return readIdentifierOrKeyword();
        }
        if (Character.isDigit(c)) {
            return readNumber();
        }
        return switch (c) {
            case '(' -> { position++; yield new Token(Token.TokenType.LPAREN, "("); }
            case ')' -> { position++; yield new Token(Token.TokenType.RPAREN, ")"); }
            case ',' -> { position++; yield new Token(Token.TokenType.COMMA, ","); }
            case '%' -> { position++; yield new Token(Token.TokenType.PERCENT, "%"); }
            case '>' -> { position++; yield new Token(Token.TokenType.GREATER_THAN, ">"); }
            case '<' -> { position++; yield new Token(Token.TokenType.LESS_THAN, "<"); }
            case '=' -> readEquals();
            default -> throw new RuleParseException(
                "Unexpected character '%s' at position %d - rule expressions may only contain letters, digits, '_', '.', '(', ')', ',', '%%', '>', '<', '=='."
                    .formatted(c, position));
        };
    }

    private Token readEquals() {
        if (position + 1 < source.length() && source.charAt(position + 1) == '=') {
            position += 2;
            return new Token(Token.TokenType.EQUALS, "==");
        }
        throw new RuleParseException("Unexpected '=' at position %d - did you mean '=='?".formatted(position));
    }

    private Token readIdentifierOrKeyword() {
        int start = position;
        while (position < source.length() && (Character.isLetterOrDigit(source.charAt(position)) || source.charAt(position) == '_')) {
            position++;
        }
        String text = source.substring(start, position);
        String upper = text.toUpperCase();
        if (KEYWORDS.contains(upper)) {
            Token.TokenType type = switch (upper) {
                case "AND" -> Token.TokenType.AND;
                case "OR" -> Token.TokenType.OR;
                case "NOT" -> Token.TokenType.NOT;
                case "CROSSES_ABOVE" -> Token.TokenType.CROSSES_ABOVE;
                case "CROSSES_BELOW" -> Token.TokenType.CROSSES_BELOW;
                default -> throw new IllegalStateException("Unreachable: " + upper);
            };
            return new Token(type, upper);
        }
        return new Token(Token.TokenType.IDENTIFIER, text);
    }

    private Token readNumber() {
        int start = position;
        while (position < source.length() && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) {
            position++;
        }
        return new Token(Token.TokenType.NUMBER, source.substring(start, position));
    }

    private void skipWhitespace() {
        while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
            position++;
        }
    }
}
