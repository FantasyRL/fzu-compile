# Work2 语法分析器设计方案

## 目标

实验二需要实现 Mandrill v2.0 的语法分析器。程序从标准输入读取 Mandrill 源代码，先调用实验一词法分析器得到 `List<Token>`，再判断 Token 序列是否符合 Mandrill 语法。

输出规则：

```text
Pass
```

或：

```text
Error
```

实验二只判断语法结构是否正确，不做变量类型、作用域、函数是否已定义等语义检查。

## 当前链路

入口文件：

- `src/main/java/cn/edu/fzu/ccds/compilerprinciples/mandrill/parser/ParserMain.java`

执行链路：

```text
stdin 源代码
    |
    v
HandcraftLexer.scanTokens()
    |
    v
List<Token>
    |
    v
HandcraftParser.parse()
    |
    v
stdout: Pass / Error
```

相关文件：

- `lexer/HandcraftLexer.java`：实验一词法分析器，必须先保证可输出完整 Token 流。
- `parser/Parser.java`：语法分析器接口，定义 `boolean parse()`。
- `parser/HandcraftParser.java`：实验二主要实现位置。
- `parser/ParserMain.java`：评测入口，不建议改输出文本。

## 语法范围

`HandcraftParser` 需要识别以下顶层结构：

```text
Program ::= { FunctionDef | Statement } EOF
FunctionDef ::= FUNC [ LBRACKET RBRACKET ] IDENTIFIER LPAREN [ ParameterList ] RPAREN Block
ParameterList ::= Parameter { COMMA Parameter }
Parameter ::= IDENTIFIER [ LBRACKET RBRACKET ]
```

语句：

```text
Statement ::= AssignmentStmt
            | IfStmt
            | WhileStmt
            | JumpStmt
            | DeclarationStmt
            | Block
            | SEMI

Block ::= LBRACE { Statement } RBRACE

IfStmt ::= IF LPAREN Expression RPAREN Block [ ELSE Block ]
WhileStmt ::= WHILE LPAREN Expression RPAREN Block

JumpStmt ::= BREAK SEMI
           | CONTINUE SEMI
           | RETURN Expression SEMI

DeclarationStmt ::= (LOCAL | GLOBAL) IDENTIFIER [ LBRACKET RBRACKET ] SEMI
```

赋值语句：

```text
AssignmentStmt ::= LValue ASSIGN Expression SEMI
                 | IDENTIFIER LBRACKET RBRACKET ASSIGN Expression SEMI

LValue ::= IDENTIFIER [ LBRACKET Expression RBRACKET ]
         | WRITE
         | PUT
```

表达式按优先级递归下降：

```text
Expression ::= Equality
Equality ::= Relational { (EQ | NEQ) Relational }
Relational ::= Additive { (LT | LTE | GT | GTE) Additive }
Additive ::= Multiplicative { (PLUS | MINUS) Multiplicative }
Multiplicative ::= Primary { (STAR | SLASH | MOD) Primary }

Primary ::= INT_CONST
          | CHAR_CONST
          | STRING_CONST
          | READ
          | GET
          | LPAREN Expression RPAREN
          | IDENTIFIER FunctionOrAccessTail

FunctionOrAccessTail ::= LPAREN [ ArgumentList ] RPAREN
                       | [ LBRACKET Expression RBRACKET ]

ArgumentList ::= Expression { COMMA Expression }
```

## 数据结构和方法规划

`HandcraftParser` 保留 Token 流、游标，并显式维护一个解析上下文栈：

```java
private final List<Token> tokens;
private final Deque<ParseContext> contexts = new ArrayDeque<>();
private int current = 0;
```

上下文类型：

```java
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
```

主要方法：

```java
public boolean parse();
private boolean withContext(ParseContext context, ParseRule rule);

private boolean program();
private boolean functionDef();
private boolean parameterList();
private boolean parameter();
private boolean statement();
private boolean block();
private boolean assignmentStmt();
private boolean ifStmt();
private boolean whileStmt();
private boolean jumpStmt();
private boolean declarationStmt();

private boolean expression();
private boolean equality();
private boolean relational();
private boolean additive();
private boolean multiplicative();
private boolean primary();
private boolean argumentList();
private boolean lValue();

private boolean match(TokenType... types);
private boolean check(TokenType type);
private boolean consume(TokenType type);
private Token advance();
private Token peek();
private Token previous();
private boolean isAtEnd();
private int mark();
private void reset(int mark);
```

`withContext` 负责 `push/pop`，保证进入函数、代码块、if、while、赋值、函数调用、数组访问、括号表达式等结构时都有明确上下文。`mark/reset` 只用于少量可选结构，例如 `[]` 后缀。

## 实现策略

采用手写递归下降 parser：

- `parse()` 调用 `program()`，最后要求消费到 `EOF`。
- 每个非终结符返回 `true/false`，失败时不抛异常，最终由 `ParserMain` 输出 `Error`。
- 语句入口按首 Token 分发：
  - `{` -> `block`
  - `if` -> `ifStmt`
  - `while` -> `whileStmt`
  - `break/continue/return` -> `jumpStmt`
  - `local/global` -> `declarationStmt`
  - `;` -> 空语句
  - `identifier/write/put` -> `assignmentStmt`
- 表达式使用循环处理左结合运算，避免左递归。
- 不检查语义，例如 `break` 是否在循环内、变量是否定义、函数参数个数是否匹配。
- `Deque<ParseContext>` 表示解析上下文栈，不表示运行时函数调用栈；函数调用只检查语法形式，不分配局部变量空间。

## 需要特别注意的样例

正常样例目录：

- `mandrill-src/*.mds` 应全部输出 `Pass`

错误样例目录：

- `mandrill-badsrc/*.mds` 应全部输出 `Error`

从文件名看，错误样例重点覆盖：

- 括号缺失或函数调用括号错误
- 空数组下标，如 `a[]` 出现在不允许的位置
- `if` 结构不完整
- 复杂表达式或语句缺少必要分隔符

## 测试方案

编译：

```bash
make clean
make
```

运行 lab2 测试脚本：

```bash
bash main.sh parser-test.log
```

通过标准：

- `mandrill-src/*.mds` 全部输出 `Pass`
- `mandrill-badsrc/*.mds` 全部输出 `Error`
- 日志最后分数应等于正常样例数加错误样例数

也可以单独测试：

```bash
java -cp build cn.edu.fzu.ccds.compilerprinciples.mandrill.parser.ParserMain < mandrill-src/01-a+1.mds
java -cp build cn.edu.fzu.ccds.compilerprinciples.mandrill.parser.ParserMain < mandrill-badsrc/a1-if.mds
```

## 实现顺序

1. 补充 `HandcraftParser` 的游标和 Token 操作方法。
2. 实现 `program/block/statement` 主结构。
3. 实现声明、跳转、if、while。
4. 实现赋值语句和左值解析。
5. 实现表达式优先级解析。
6. 跑 `bash main.sh parser-test.log`，按失败样例补齐边界。

当前样例验证结果：

- `make clean && make` 编译通过。
- `bash main.sh parser-test.log` 通过 15 个样例，其中正常样例 10 个输出 `Pass`，错误样例 5 个输出 `Error`。
