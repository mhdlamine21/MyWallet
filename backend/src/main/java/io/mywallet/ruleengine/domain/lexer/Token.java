package io.mywallet.ruleengine.domain.lexer;

public record Token(TokenType type, String text) {

    public enum TokenType {
        IDENTIFIER,     // SMA, RSI, PortfolioExposure, BTCUSDT, ...
        NUMBER,         // 20, 14, 50.5
        PERCENT,        // the '%' suffix on a number
        AND, OR, NOT,
        GREATER_THAN,   // >
        LESS_THAN,      // <
        EQUALS,         // ==
        CROSSES_ABOVE,  // keyword: CROSSES_ABOVE
        CROSSES_BELOW,  // keyword: CROSSES_BELOW
        LPAREN, RPAREN,
        COMMA,
        EOF
    }
}
