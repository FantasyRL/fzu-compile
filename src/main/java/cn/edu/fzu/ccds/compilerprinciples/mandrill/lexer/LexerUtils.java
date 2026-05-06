package cn.edu.fzu.ccds.compilerprinciples.mandrill.lexer;

import java.io.IOException;

final class LexerUtils {
    static final int EOF = -1;

    private LexerUtils() {
    }

    @FunctionalInterface
    interface CharReader {
        int read() throws IOException;
    }

    // 已经读到 # 后调用，持续吞掉字符直到换行或文件结束。
    static void skipLineComment(CharReader reader) throws IOException {
        int c;
        while ((c = reader.read()) != EOF && c != '\n') {
            // Skip comment body.
        }
    }

    // 以下字符分类函数直接对应 Mandrill 的词法定义。
    static boolean isIdentifierStart(int c) {
        return c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    static boolean isIdentifierPart(int c) {
        return isIdentifierStart(c) || isDigit(c);
    }

    static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    static boolean isValidCharEscape(int c) {
        return c == 'n' || c == '\\' || c == '\'';
    }

    static boolean isValidStringEscape(int c) {
        return c == 'n' || c == '\\' || c == '"' || c == '\'';
    }

    // 把不可见字符转成可读文本，用于错误信息。
    static String printable(int c) {
        if (c == '\t') {
            return "\\t";
        }
        if (c == '\r') {
            return "\\r";
        }
        if (c == '\n') {
            return "\\n";
        }
        return Character.toString((char) c);
    }
}
