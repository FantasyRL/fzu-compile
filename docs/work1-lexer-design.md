# Work1 词法分析器设计方案

## 目标

实验一需要实现 Mandrill v2.0 的手写词法分析器。程序从标准输入或文件读取源代码，将字符流切分为 Token 流，并按照 `Token.toString()` 规定的格式输出：

```text
Ln 1  , Col 1   | WRITE           'write'
Ln 1  , Col 7   | ASSIGN          '='
Ln 1  , Col 9   | READ            'read'
Ln 1  , Col 14  | PLUS            '+'
Ln 1  , Col 16  | INT_CONST       '1'
Ln 1  , Col 17  | SEMI            ';'
Ln 2  , Col 1   | EOF             '<EOF>'
```

实现范围聚焦在 `HandcraftLexer.scanTokens()`，不改动 `main()` 和已有输出格式。

## 计划新增或调整的文件

### 1. `src/main/java/mandrill/HandcraftLexer.java`

这是唯一需要实现核心逻辑的源码文件。计划补充：

- `reader` 字段，保存构造函数中创建的 `PushbackReader`。
- `tokens` 字段或局部列表，保存扫描出的 Token。
- `line`、`column` 字段，维护当前读取位置。
- 关键字表 `KEYWORDS`。
- 字符读取、回退、加 Token、报错等私有辅助方法。
- 完整实现 `scanTokens()`。

### 2. `src/main/java/mandrill/TokenType.java`

当前枚举已经覆盖实验一需要的 Token 类型，原则上不需要新增类型：

- 关键字：`IF`、`ELSE`、`WHILE`、`READ`、`PUT`、`WRITE`、`GET`、`FUNC`、`GLOBAL`、`LOCAL`、`RETURN`、`BREAK`、`CONTINUE`
- 字面量与标识符：`IDENTIFIER`、`INT_CONST`、`CHAR_CONST`、`STRING_CONST`
- 运算符：`PLUS`、`MINUS`、`STAR`、`SLASH`、`MOD`、`ASSIGN`、`EQ`、`NEQ`、`LT`、`LTE`、`GT`、`GTE`
- 界符：`LPAREN`、`RPAREN`、`LBRACKET`、`RBRACKET`、`LBRACE`、`RBRACE`、`COMMA`、`SEMI`
- 结束和错误：`EOF`、`ERROR`

### 3. `src/main/java/mandrill/Token.java`

当前数据结构和输出格式已经符合样例，不需要调整。

### 4. `docs/work1-lexer-design.md`

本文件记录实现方案，供后续编码时对照。

## 数据结构设计

### Token

沿用现有 `Token`：

```java
public class Token {
    public final TokenType type;
    public final String lexeme;
    public final int line;
    public final int column;
}
```

含义：

- `type`：Token 类型。
- `lexeme`：源码中的词素文本。字符串常量不包含两侧双引号，字符常量保留单引号内的内容形式，例如 `\n`。
- `line`：Token 起始行号，从 1 开始。
- `column`：Token 起始列号，从 1 开始。

### Lexer 状态

在 `HandcraftLexer` 中维护：

```java
private final PushbackReader reader;
private final List<Token> tokens = new ArrayList<>();
private int line = 1;
private int column = 0;
```

位置规则：

- `readChar()` 每读入一个字符就更新位置。
- 普通字符：`column += 1`。
- 换行 `\n`：`line += 1`，`column = 0`。
- 回退字符时同步恢复位置；实现上可以只在需要向前看一个字符时使用 `unreadChar(c, oldLine, oldColumn)`，保存读前位置以避免行列错乱。

### 关键字表

使用静态 Map 将词素映射到 TokenType：

```java
private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
    Map.entry("if", TokenType.IF),
    Map.entry("else", TokenType.ELSE),
    Map.entry("while", TokenType.WHILE),
    Map.entry("read", TokenType.READ),
    Map.entry("put", TokenType.PUT),
    Map.entry("write", TokenType.WRITE),
    Map.entry("get", TokenType.GET),
    Map.entry("func", TokenType.FUNC),
    Map.entry("global", TokenType.GLOBAL),
    Map.entry("local", TokenType.LOCAL),
    Map.entry("return", TokenType.RETURN),
    Map.entry("break", TokenType.BREAK),
    Map.entry("continue", TokenType.CONTINUE)
);
```

扫描到标识符后，先查 `KEYWORDS`。命中则输出关键字 Token，否则输出 `IDENTIFIER`。

## 常量和词法规则

### 字符分类

计划提供私有方法：

```java
private boolean isIdentifierStart(int c);
private boolean isIdentifierPart(int c);
private boolean isDigit(int c);
private boolean isWhitespace(int c);
```

规则：

- 标识符开头：`a-z`、`A-Z`、`_`。
- 标识符后续：标识符开头字符或 `0-9`。
- 整数：一个或多个数字。
- 空白：空格、`\t`、`\r`、`\n`。

### 注释

`#` 开始直到行尾为注释。扫描器读到 `#` 后继续读取并丢弃字符，直到遇到 `\n` 或 EOF。

### 字符常量

格式：

```text
'a'
'\n'
'\\'
'\''
```

输出为 `CHAR_CONST`，`lexeme` 不包含外侧单引号，保留转义文本，例如 `\n`。

错误场景：

- 缺少闭合单引号。
- 空字符常量。
- 非法转义。
- 字符常量中直接跨行。

### 字符串常量

格式：

```text
"Hello World"
"World \n"
```

输出为 `STRING_CONST`，`lexeme` 不包含外侧双引号。扫描阶段只负责识别字符串文本，不在实验一中实际转换 UTF-32。

支持的转义至少包括：

- `\n`
- `\\`
- `\"`

为了兼容字符常量和后续实验，也可以接受 `\'` 并按原样保留。

错误场景：

- 未闭合字符串。
- 字符串中直接跨行。
- 非法转义。

### 运算符和界符

单字符 Token：

| 字符 | TokenType |
| --- | --- |
| `+` | `PLUS` |
| `-` | `MINUS` |
| `*` | `STAR` |
| `/` | `SLASH` |
| `%` | `MOD` |
| `(` | `LPAREN` |
| `)` | `RPAREN` |
| `[` | `LBRACKET` |
| `]` | `RBRACKET` |
| `{` | `LBRACE` |
| `}` | `RBRACE` |
| `,` | `COMMA` |
| `;` | `SEMI` |

可能由两个字符组成的 Token：

| 输入 | TokenType |
| --- | --- |
| `=` | `ASSIGN` |
| `==` | `EQ` |
| `!` | 非法 |
| `!=` | `NEQ` |
| `<` | `LT` |
| `<=` | `LTE` |
| `>` | `GT` |
| `>=` | `GTE` |

## 链路打通方案

整体链路如下：

```text
stdin / file
    |
    v
HandcraftLexer(InputStream)
    |
    v
PushbackReader 按字符读取
    |
    v
scanTokens()
    |
    +-- 跳过空白和注释
    +-- 识别关键字 / 标识符
    +-- 识别整数常量
    +-- 识别字符常量
    +-- 识别字符串常量
    +-- 识别运算符和界符
    +-- 遇到非法输入时报错并退出
    |
    v
List<Token>
    |
    v
main() 逐行 print token.toString()
    |
    v
stdout / 输出文件
```

`main()` 已经完成输入输出链路：

```java
InputStream inputStream = args.length > 0 && !args[0].equals("-")
    ? new FileInputStream(args[0])
    : System.in;
PrintStream printStream = args.length > 1 && !args[1].equals("-")
    ? new PrintStream(args[1])
    : System.out;
```

因此实现时只需要保证 `scanTokens()` 返回正确的 `List<Token>`。

## `scanTokens()` 流程

伪代码：

```java
public List<Token> scanTokens() {
    while (true) {
        int c = readChar();
        if (c == EOF) {
            addToken(TokenType.EOF, "<EOF>", line, column + 1);
            return tokens;
        }

        if (isWhitespace(c)) {
            continue;
        }

        if (c == '#') {
            skipLineComment();
            continue;
        }

        int startLine = line;
        int startColumn = column;

        if (isIdentifierStart(c)) {
            scanIdentifierOrKeyword(c, startLine, startColumn);
            continue;
        }

        if (isDigit(c)) {
            scanInteger(c, startLine, startColumn);
            continue;
        }

        switch (c) {
            case '\'': scanCharConstant(startLine, startColumn); break;
            case '"': scanStringConstant(startLine, startColumn); break;
            case '=': addToken(match('=') ? EQ : ASSIGN, ...); break;
            case '!': requireMatch('=', NEQ); break;
            case '<': addToken(match('=') ? LTE : LT, ...); break;
            case '>': addToken(match('=') ? GTE : GT, ...); break;
            default: handleSingleCharTokenOrError(c, startLine, startColumn);
        }
    }
}
```

EOF 列号按当前样例处理：如果最后一个有效字符是换行，则 EOF 位于下一行第 1 列。

## 错误处理

实验要求非法输入给出清晰错误并安全退出，计划采用：

```java
private void error(String message, int line, int column) {
    System.err.printf("Lexer error at Ln %d, Col %d: %s%n", line, column, message);
    System.exit(1);
}
```

错误不会作为 `ERROR` Token 混入正常输出，避免影响评测对 stdout 的 diff。

## 测试方案

### 1. 编译检查

```bash
make clean
make
```

### 2. 现有样例回归

```bash
./main.sh lexer-test.log
```

通过标准：

- `mandrill-src/*.mds` 的输出与 `mandrill-lexerout/*.lexerout` 完全一致。
- 日志最后的分数应等于测试样例数量。

### 3. 补充手工测试

建议补充临时输入覆盖：

- 注释：`# comment`
- 复合运算符：`a <= b; a >= b; a == b; a != b;`
- 字符常量：`'\n'`, `'\\'`, `'\''`
- 字符串常量：`"Hello World"`
- 非法输入：未闭合字符串、单独的 `!`、未知字符。

## 实现顺序

1. 补齐 `reader`、`tokens`、`line`、`column` 等字段。
2. 实现读取、回退、匹配、添加 Token、报错方法。
3. 实现空白和注释跳过。
4. 实现标识符/关键字、整数常量。
5. 实现字符常量和字符串常量。
6. 实现运算符和界符。
7. 跑现有 `main.sh`，根据 diff 修正行列号和 lexeme 细节。
