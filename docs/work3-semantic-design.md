# Work3 语义分析器设计方案

## 当前导入内容

已从实验 3 模板目录导入以下内容：

- `Mandrill.g4`：ANTLR4 语法文件，用来生成 Mandrill Lexer/Parser。
- `antlr-4.13.2-complete.jar`：ANTLR 运行时和代码生成工具。
- `mandrill-semantic-error-src/`：语义错误测试样例。
- `src/main/java/cn/edu/fzu/ccds/compilerprinciples/mandrill/semanticchecker/`：语义检查器入口和异常类脚手架。
- `Makefile`、`main.sh`：实验 3 的构建和测试入口，当前入口类为 `SemanticCheckerMain`。

模板中缺失的语义类已经用 `XK` 后缀补齐，例如 `MandrillTypeXK`、`SymbolTableXK`、`SymbolCollectorXK`、`SemanticCheckerXK`。

## 实验 3 要做什么

实验 1 处理字符到 Token，实验 2 判断语法结构是否合法。实验 3 的目标是：在语法正确的前提下，进一步判断程序是否符合语义规则。

输出仍然只有：

```text
Pass
```

或：

```text
Error
```

实验 3 重点不是“语法长得对不对”，而是“这段程序按 Mandrill 语言规则是否说得通”。

示例：

```c
a[] = "test";
a = 5;
```

语法上这是合法赋值，但语义上错误：`a` 已经被推断为数组变量，后续不能直接赋整数。

## 当前运行链路

```text
stdin / file
    |
    v
ANTLR CharStream
    |
    v
MandrillLexer
    |
    v
CommonTokenStream
    |
    v
MandrillParser.program()
    |
    v
Parse Tree
    |
    v
SymbolCollectorXK 收集符号与函数信息
    |
    v
SemanticCheckerXK 检查类型和调用规则
    |
    v
Pass / Error
```

## 需要检查的核心语义

结合 `mandrill-semantic-error-src/`，至少需要支持以下检查：

- 数组变量不能被当作普通整型变量直接赋整型值。
- 数组变量不能直接执行 `get` / `put` 这类字符/整数操作。
- 整型变量不能传给数组形参。
- 数组变量不能传给整型形参。
- 函数返回类型要区分整型和数组。
- 函数实参与形参类型需要匹配。
- `break` / `continue` 必须位于循环体内。
- `return` 必须位于函数体内。
- `if` / `while` 条件表达式必须是整型。
- 数组元素赋值右侧必须是整型。

同时需要接受正常样例：

- 普通整数读写和表达式。
- `if` / `while` 控制流。
- 字符串和数组变量。
- 函数定义、函数调用、递归调用。
- `local` / `global` 声明和作用域切换。

## 已新增类型和文件

新增实现遵守 `XK` 后缀命名：

### 1. `MandrillTypeXK.java`

表示表达式和变量类型：

```java
enum MandrillTypeXK {
    INT,
    ARRAY,
    UNKNOWN
}
```

### 2. `SymbolXK.java`

表示变量或函数符号：

```java
class SymbolXK {
    String name;
    MandrillTypeXK type;
    SymbolKindXK kind;
}
```

### 3. `SymbolKindXK.java`

区分符号类别：

```java
enum SymbolKindXK {
    VARIABLE,
    FUNCTION
}
```

### 4. `FunctionSymbolXK.java`

记录函数签名：

```java
class FunctionSymbolXK {
    String name;
    MandrillTypeXK returnType;
    List<MandrillTypeXK> parameterTypes;
}
```

### 5. `ScopeXK.java`

表示一个作用域：

```java
class ScopeXK {
    Map<String, SymbolXK> variables;
}
```

### 6. `SymbolTableXK.java`

维护全局函数表和作用域栈：

```java
class SymbolTableXK {
    Map<String, FunctionSymbolXK> functions;
    Deque<ScopeXK> scopes;
}
```

### 7. `SymbolCollectorXK.java`

第一遍遍历 parse tree，收集函数定义、形参和显式声明。

### 8. `SemanticCheckerXK.java`

第二遍遍历 parse tree，推断表达式类型并检查赋值、调用、返回值等规则。

也可以复用模板里的 `SemanticChecker`，但如果由我新增实现，应改成 `SemanticCheckerXK` 并同步修改入口。

## 两遍遍历策略

建议分两遍：

### 第一遍：符号收集

目标：

- 收集所有函数签名，支持递归和先调用后定义的情况。
- 建立全局作用域。
- 记录函数参数类型，例如 `a[]` 是数组参数，`a` 是整型参数。

第一遍不做复杂表达式检查，只建立后续检查需要的基础信息。

### 第二遍：语义检查

目标：

- 进入函数时创建局部作用域。
- 进入代码块时创建嵌套作用域。
- 根据赋值、声明、字符串字面量、数组声明推断变量类型。
- 计算表达式类型。
- 检查赋值左右类型是否兼容。
- 检查函数调用参数类型。
- 检查 `return` 类型是否符合函数声明。

发现错误时抛出 `SemanticException`，入口捕获后输出 `Error`。

## 当前编译和测试状态

使用 Homebrew OpenJDK 17 临时设置：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
make clean && make
```

ANTLR 代码生成、Java 编译和实验 3 样例测试均已通过。

## 测试方案

运行：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
bash main.sh semantic-test.log
```

当前结果：

- `mandrill-src/*.mds` 全部输出 `Pass`
- `mandrill-semantic-error-src/*.mds` 全部输出 `Error`
- `bash main.sh semantic-test.log` 得分 `14`

当前语义错误样例覆盖：

- `01-array-assigned-int.mds`：数组变量被赋整型。
- `02-array-get-put.mds`：数组变量参与 `get` / `put`。
- `03-int-to-array-param.mds`：整型实参传给数组形参。
- `04-array-to-int-param.mds`：数组实参传给整型形参。

## 已完成实现

1. `SemanticCheckerMain` 已改为调用 `SymbolCollectorXK`、`SymbolTableXK`、`SemanticCheckerXK`。
2. 已新增类型系统和符号表文件，全部追加 `XK` 后缀。
3. 已实现函数签名收集。
4. 已实现变量类型推断和作用域查询。
5. 已实现表达式类型推断。
6. 已实现赋值、函数调用、返回值检查。
7. 已补充循环上下文、全局 `return`、条件表达式类型、数组元素赋值类型检查。
8. 已通过 `bash main.sh semantic-test.log`。
