package cn.edu.fzu.ccds.compilerprinciples.mandrill.parser;

import cn.edu.fzu.ccds.compilerprinciples.mandrill.lexer.tokens.Token;
import cn.edu.fzu.ccds.compilerprinciples.mandrill.lexer.tokens.TokenType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class HandcraftParser implements Parser {
    private enum ParseContext {
        PROGRAM,
        FUNCTION_DEF,
        BLOCK,
        IF_STMT,
        WHILE_STMT,
        DECLARATION,
        ASSIGNMENT,
        RETURN_STMT,
        CALL_EXPR,
        ARRAY_ACCESS,
        PAREN_EXPR
    }

    @FunctionalInterface
    private interface ParseRule {
        boolean parse();
    }

    private final List<Token> tokens;
    private final Deque<ParseContext> contexts = new ArrayDeque<>();
    private int current = 0;

    public HandcraftParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean parse() {
        return withContext(ParseContext.PROGRAM, this::program);
    }

    // 统一维护解析上下文栈：进入一个语法结构时入栈，结束时出栈。
    private boolean withContext(ParseContext context, ParseRule rule) {
        contexts.push(context);
        try {
            return rule.parse();
        } finally {
            contexts.pop();
        }
    }

    // Program ::= { FunctionDef | Statement } EOF
    private boolean program() {
        while (!isAtEnd()) {
            if (check(TokenType.FUNC)) {
                if (!functionDef()) {
                    return false;
                }
            } else if (!statement()) {
                return false;
            }
        }
        return true;
    }

    // FunctionDef ::= FUNC [ [] ] IDENTIFIER ( [ParameterList] ) Block
    private boolean functionDef() {
        return withContext(ParseContext.FUNCTION_DEF, () -> {
            if (!consume(TokenType.FUNC)) {
                return false;
            }
            matchArraySuffix();
            if (!consume(TokenType.IDENTIFIER) || !consume(TokenType.LPAREN)) {
                return false;
            }
            if (!check(TokenType.RPAREN) && !parameterList()) {
                return false;
            }
            return consume(TokenType.RPAREN) && block();
        });
    }

    private boolean parameterList() {
        if (!parameter()) {
            return false;
        }
        while (match(TokenType.COMMA)) {
            if (!parameter()) {
                return false;
            }
        }
        return true;
    }

    private boolean parameter() {
        if (!consume(TokenType.IDENTIFIER)) {
            return false;
        }
        matchArraySuffix();
        return true;
    }

    // Statement 按首 Token 分派，避免每种语句互相试探。
    private boolean statement() {
        if (match(TokenType.SEMI)) {
            return true;
        }
        if (check(TokenType.LBRACE)) {
            return block();
        }
        if (check(TokenType.IF)) {
            return ifStmt();
        }
        if (check(TokenType.WHILE)) {
            return whileStmt();
        }
        if (check(TokenType.BREAK) || check(TokenType.CONTINUE) || check(TokenType.RETURN)) {
            return jumpStmt();
        }
        if (check(TokenType.LOCAL) || check(TokenType.GLOBAL)) {
            return declarationStmt();
        }
        if (check(TokenType.IDENTIFIER) || check(TokenType.WRITE) || check(TokenType.PUT)) {
            return assignmentStmt();
        }
        return false;
    }

    // Block ::= { { Statement } }
    private boolean block() {
        return withContext(ParseContext.BLOCK, () -> {
            if (!consume(TokenType.LBRACE)) {
                return false;
            }
            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                if (!statement()) {
                    return false;
                }
            }
            return consume(TokenType.RBRACE);
        });
    }

    // IfStmt ::= if ( Expression ) Block [ else Block ]
    private boolean ifStmt() {
        return withContext(ParseContext.IF_STMT, () -> {
            if (!consume(TokenType.IF) || !consume(TokenType.LPAREN) || !expression() || !consume(TokenType.RPAREN)) {
                return false;
            }
            if (!block()) {
                return false;
            }
            return !match(TokenType.ELSE) || block();
        });
    }

    // WhileStmt ::= while ( Expression ) Block
    private boolean whileStmt() {
        return withContext(ParseContext.WHILE_STMT, () ->
                consume(TokenType.WHILE)
                        && consume(TokenType.LPAREN)
                        && expression()
                        && consume(TokenType.RPAREN)
                        && block()
        );
    }

    // JumpStmt ::= break; | continue; | return Expression;
    private boolean jumpStmt() {
        if (match(TokenType.BREAK) || match(TokenType.CONTINUE)) {
            return consume(TokenType.SEMI);
        }
        if (match(TokenType.RETURN)) {
            return withContext(ParseContext.RETURN_STMT, () -> expression() && consume(TokenType.SEMI));
        }
        return false;
    }

    // DeclarationStmt ::= (local | global) IDENTIFIER [ [] ] ;
    private boolean declarationStmt() {
        return withContext(ParseContext.DECLARATION, () -> {
            if (!match(TokenType.LOCAL, TokenType.GLOBAL) || !consume(TokenType.IDENTIFIER)) {
                return false;
            }
            matchArraySuffix();
            return consume(TokenType.SEMI);
        });
    }

    // AssignmentStmt ::= LValue = Expression ; | IDENTIFIER [] = Expression ;
    private boolean assignmentStmt() {
        return withContext(ParseContext.ASSIGNMENT, () -> {
            if (!lValue()) {
                return false;
            }
            return consume(TokenType.ASSIGN) && expression() && consume(TokenType.SEMI);
        });
    }

    private boolean lValue() {
        if (match(TokenType.WRITE, TokenType.PUT)) {
            return true;
        }
        if (!consume(TokenType.IDENTIFIER)) {
            return false;
        }
        if (match(TokenType.LBRACKET)) {
            if (match(TokenType.RBRACKET)) {
                return true;
            }
            return withContext(ParseContext.ARRAY_ACCESS, () -> expression() && consume(TokenType.RBRACKET));
        }
        return true;
    }

    private boolean expression() {
        return equality();
    }

    private boolean equality() {
        if (!relational()) {
            return false;
        }
        while (match(TokenType.EQ, TokenType.NEQ)) {
            if (!relational()) {
                return false;
            }
        }
        return true;
    }

    private boolean relational() {
        if (!additive()) {
            return false;
        }
        while (match(TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE)) {
            if (!additive()) {
                return false;
            }
        }
        return true;
    }

    private boolean additive() {
        if (!multiplicative()) {
            return false;
        }
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            if (!multiplicative()) {
                return false;
            }
        }
        return true;
    }

    private boolean multiplicative() {
        if (!primary()) {
            return false;
        }
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.MOD)) {
            if (!primary()) {
                return false;
            }
        }
        return true;
    }

    private boolean primary() {
        if (match(TokenType.INT_CONST, TokenType.CHAR_CONST, TokenType.STRING_CONST, TokenType.READ, TokenType.GET)) {
            return true;
        }
        if (match(TokenType.LPAREN)) {
            return withContext(ParseContext.PAREN_EXPR, () -> expression() && consume(TokenType.RPAREN));
        }
        if (match(TokenType.IDENTIFIER)) {
            if (match(TokenType.LPAREN)) {
                return withContext(ParseContext.CALL_EXPR, () -> {
                    if (!check(TokenType.RPAREN) && !argumentList()) {
                        return false;
                    }
                    return consume(TokenType.RPAREN);
                });
            }
            if (match(TokenType.LBRACKET)) {
                return withContext(ParseContext.ARRAY_ACCESS, () -> expression() && consume(TokenType.RBRACKET));
            }
            return true;
        }
        return false;
    }

    private boolean argumentList() {
        if (!expression()) {
            return false;
        }
        while (match(TokenType.COMMA)) {
            if (!expression()) {
                return false;
            }
        }
        return true;
    }

    private void matchArraySuffix() {
        int mark = mark();
        if (!(match(TokenType.LBRACKET) && consume(TokenType.RBRACKET))) {
            reset(mark);
        }
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean consume(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return current >= tokens.size() || tokens.get(current).type == TokenType.EOF;
    }

    private int mark() {
        return current;
    }

    private void reset(int mark) {
        current = mark;
    }
}
