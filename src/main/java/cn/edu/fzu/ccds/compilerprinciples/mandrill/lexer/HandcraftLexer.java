package cn.edu.fzu.ccds.compilerprinciples.mandrill.lexer;

import cn.edu.fzu.ccds.compilerprinciples.mandrill.lexer.tokens.Token;
import cn.edu.fzu.ccds.compilerprinciples.mandrill.lexer.tokens.TokenType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandcraftLexer {
    
    public List<Token> scanTokens() {
        // 需要实现的核心函数
        return tokens;
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
