package mandrill;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandcraftLexer {
    // 关键字表 先按标识符规则读出完整单词，再用这张表判断是否为关键字。
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    // init
    static {
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("read", TokenType.READ);
        KEYWORDS.put("put", TokenType.PUT);
        KEYWORDS.put("write", TokenType.WRITE);
        KEYWORDS.put("get", TokenType.GET);
        KEYWORDS.put("func", TokenType.FUNC);
        KEYWORDS.put("global", TokenType.GLOBAL);
        KEYWORDS.put("local", TokenType.LOCAL);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("break", TokenType.BREAK);
        KEYWORDS.put("continue", TokenType.CONTINUE);
    }

    private final PushbackReader reader;
    private final List<Token> tokens = new ArrayList<>();

    // 当前扫描位置。column 表示最近一次读入字符所在列；行首读字符前为 0。
    private int line = 1;
    private int column = 0;

    public List<Token> scanTokens() throws IOException {
        int c;

        // 主循环：每次先读一个字符，再根据字符类别分派给对应的扫描函数。
        while ((c = readChar()) != LexerUtils.EOF) {
            // 空白字符只负责分隔词素，不产生 Token。
            if (LexerUtils.isWhitespace(c)) {
                continue;
            }

            // Mandrill 行注释：从 # 到行尾全部丢弃。
            if (c == '#') {
                LexerUtils.skipLineComment(this::readChar);
                continue;
            }

            // Token 的位置信息记录的是词素起始位置。
            int startLine = line;
            int startColumn = column;

            // 标识符和关键字共享同一套字符规则，读完后再查关键字表。
            if (LexerUtils.isIdentifierStart(c)) {
                scanIdentifierOrKeyword(c, startLine, startColumn);
                continue;
            }

            // 整数常量由连续数字组成；负数由语法层表达为 0 - x。
            if (LexerUtils.isDigit(c)) {
                scanInteger(c, startLine, startColumn);
                continue;
            }

            // 剩余字符要么是字面量开头、运算符/界符，要么就是词法错误。
            switch (c) {
                case '\'':
                    scanCharConstant(startLine, startColumn);
                    break;
                case '"':
                    scanStringConstant(startLine, startColumn);
                    break;
                case '+':
                    addToken(TokenType.PLUS, "+", startLine, startColumn);
                    break;
                case '-':
                    addToken(TokenType.MINUS, "-", startLine, startColumn);
                    break;
                case '*':
                    addToken(TokenType.STAR, "*", startLine, startColumn);
                    break;
                case '/':
                    addToken(TokenType.SLASH, "/", startLine, startColumn);
                    break;
                case '%':
                    addToken(TokenType.MOD, "%", startLine, startColumn);
                    break;
                case '=':
                    if (match('=')) {
                        addToken(TokenType.EQ, "==", startLine, startColumn);
                    } else {
                        addToken(TokenType.ASSIGN, "=", startLine, startColumn);
                    }
                    break;
                case '!':
                    if (match('=')) {
                        addToken(TokenType.NEQ, "!=", startLine, startColumn);
                    } else {
                        error("unexpected character '!'", startLine, startColumn);
                    }
                    break;
                case '<':
                    if (match('=')) {
                        addToken(TokenType.LTE, "<=", startLine, startColumn);
                    } else {
                        addToken(TokenType.LT, "<", startLine, startColumn);
                    }
                    break;
                case '>':
                    if (match('=')) {
                        addToken(TokenType.GTE, ">=", startLine, startColumn);
                    } else {
                        addToken(TokenType.GT, ">", startLine, startColumn);
                    }
                    break;
                case '(':
                    addToken(TokenType.LPAREN, "(", startLine, startColumn);
                    break;
                case ')':
                    addToken(TokenType.RPAREN, ")", startLine, startColumn);
                    break;
                case '[':
                    addToken(TokenType.LBRACKET, "[", startLine, startColumn);
                    break;
                case ']':
                    addToken(TokenType.RBRACKET, "]", startLine, startColumn);
                    break;
                case '{':
                    addToken(TokenType.LBRACE, "{", startLine, startColumn);
                    break;
                case '}':
                    addToken(TokenType.RBRACE, "}", startLine, startColumn);
                    break;
                case ',':
                    addToken(TokenType.COMMA, ",", startLine, startColumn);
                    break;
                case ';':
                    addToken(TokenType.SEMI, ";", startLine, startColumn);
                    break;
                default:
                    error("unexpected character '" + LexerUtils.printable(c) + "'", startLine, startColumn);
            }
        }

        // EOF 也作为一个 Token 输出，列号落在当前行最后一个字符之后。
        addToken(TokenType.EOF, "<EOF>", line, column + 1);
        return tokens;
    }

    // 统一读字符并维护行列号。所有扫描逻辑都从这里取字符，避免位置计算分散。
    private int readChar() throws IOException {
        int c = reader.read();
        if (c == LexerUtils.EOF) {
            return LexerUtils.EOF;
        }
        if (c == '\n') {
            line++;
            column = 0;
        } else {
            column++;
        }
        return c;
    }

    // 向前看多读了一个字符时，把它放回 reader，同时回滚位置。
    private void unreadChar(int c) throws IOException {
        if (c == LexerUtils.EOF) {
            return;
        }
        reader.unread(c);
        if (c == '\n') {
            line--;
            column = 0;
        } else {
            column--;
        }
    }

    // 尝试匹配双字符运算符的第二个字符，例如 ==、!=、<=、>=。
    private boolean match(char expected) throws IOException {
        int c = readChar();
        if (c == expected) {
            return true;
        }
        unreadChar(c);
        return false;
    }

    // 扫描形如 abc、a1、_tmp 的词素，并区分普通标识符与保留关键字。
    private void scanIdentifierOrKeyword(int first, int startLine, int startColumn) throws IOException {
        StringBuilder lexeme = new StringBuilder();
        lexeme.append((char) first);

        int c;
        while ((c = readChar()) != LexerUtils.EOF && LexerUtils.isIdentifierPart(c)) {
            lexeme.append((char) c);
        }
        unreadChar(c);

        String text = lexeme.toString();
        addToken(KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER), text, startLine, startColumn);
    }

    // 扫描十进制整数；实验说明要求整数长度不超过 8 位。
    private void scanInteger(int first, int startLine, int startColumn) throws IOException {
        StringBuilder lexeme = new StringBuilder();
        lexeme.append((char) first);

        int c;
        while ((c = readChar()) != LexerUtils.EOF && LexerUtils.isDigit(c)) {
            lexeme.append((char) c);
        }
        unreadChar(c);

        if (lexeme.length() > 8) {
            error("integer constant is longer than 8 digits", startLine, startColumn);
        }

        addToken(TokenType.INT_CONST, lexeme.toString(), startLine, startColumn);
    }

    // 扫描单引号字符常量。lexeme 不包含外层单引号，但保留转义写法，如 \n。
    private void scanCharConstant(int startLine, int startColumn) throws IOException {
        StringBuilder lexeme = new StringBuilder();
        int c = readChar();

        if (c == LexerUtils.EOF || c == '\n') {
            error("unterminated character constant", startLine, startColumn);
        }

        if (c == '\\') {
            int escaped = readChar();
            if (!LexerUtils.isValidCharEscape(escaped)) {
                error("invalid character escape", line, column);
            }
            lexeme.append('\\').append((char) escaped);
        } else if (c == '\'') {
            error("empty character constant", startLine, startColumn);
        } else {
            lexeme.append((char) c);
        }

        int closing = readChar();
        if (closing != '\'') {
            error("unterminated character constant", startLine, startColumn);
        }

        addToken(TokenType.CHAR_CONST, lexeme.toString(), startLine, startColumn);
    }

    // 扫描双引号字符串常量。实验一只输出词素，不在这里做 UTF-32 内码转换。
    private void scanStringConstant(int startLine, int startColumn) throws IOException {
        StringBuilder lexeme = new StringBuilder();
        int c;

        while ((c = readChar()) != LexerUtils.EOF) {
            if (c == '"') {
                addToken(TokenType.STRING_CONST, lexeme.toString(), startLine, startColumn);
                return;
            }
            if (c == '\n') {
                error("unterminated string constant", startLine, startColumn);
            }
            if (c == '\\') {
                int escaped = readChar();
                if (!LexerUtils.isValidStringEscape(escaped)) {
                    error("invalid string escape", line, column);
                }
                lexeme.append('\\').append((char) escaped);
            } else {
                lexeme.append((char) c);
            }
        }

        error("unterminated string constant", startLine, startColumn);
    }

    // 所有 Token 都从这里进入结果列表，保证输出格式只由 Token.toString() 负责。
    private void addToken(TokenType type, String lexeme, int tokenLine, int tokenColumn) {
        tokens.add(new Token(type, lexeme, tokenLine, tokenColumn));
    }

    // 错误信息走 stderr，stdout 保持只输出 Token，避免影响评测 diff。
    private void error(String message, int errorLine, int errorColumn) {
        System.err.printf("Lexer error at Ln %d, Col %d: %s%n", errorLine, errorColumn, message);
        System.exit(1);
    }
    
    // 这两个函数不要进行改动
    public HandcraftLexer(InputStream is) {
        this.reader = new PushbackReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    public static void main(String[] args) throws IOException {
        InputStream inputStream = args.length > 0 && !args[0].equals("-") ? new FileInputStream(args[0]) : System.in;
        PrintStream printStream = args.length > 1 && !args[1].equals("-") ? new PrintStream(args[1]) : System.out;
        try (inputStream; printStream) {
            HandcraftLexer lexer = new HandcraftLexer(inputStream);
            List<Token> tokens = lexer.scanTokens();

            for (Token token : tokens) {
                printStream.println(token);
            }
        }
    }
}
