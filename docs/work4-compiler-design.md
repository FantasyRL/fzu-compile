# Work4 虚拟机汇编代码生成设计方案

## 当前导入内容

已从实验 4 模板目录导入以下内容：

- `Mandrill.g4`：ANTLR4 语法文件。
- `antlr-4.13.2-complete.jar`：ANTLR 生成器和运行时。
- `src/main/java/cn/edu/fzu/ccds/compilerprinciples/mandrill/compiler/`：编译器入口和接口模板。
- `src/main/java/cn/edu/fzu/ccds/compilerprinciples/mandrill/simulator/`：课程提供的虚拟机模拟器、汇编解析器、堆和指令实现。
- `mandrill-src/`：实验 4 源程序样例。
- `mandrill-in/`：运行输入。
- `mandrill-ans/`：期望运行输出。
- `Makefile`、`main.sh`：实验 4 构建和测试脚本。

后续由我新增的函数和类继续遵守 `XK` 后缀命名，例如 `CompilerImplXK`、`CodeGeneratorXK`、`emitXK`。

## 实验 4 要做什么

实验 4 的目标是把 Mandrill 源代码编译成课程虚拟机可执行的汇编代码。

整体流程：

```text
Mandrill 源代码
    |
    v
ANTLR Lexer / Parser
    |
    v
Parse Tree
    |
    v
语义检查
    |
    v
CompilerImplXK / CodeGeneratorXK
    |
    v
虚拟机汇编 data.asm
    |
    v
SimulatorMain 执行 data.asm + mandrill.in
    |
    v
data.out
```

实验 1 到实验 3 都是前端：

- 实验 1：字符流到 Token。
- 实验 2：Token 到合法语法结构。
- 实验 3：语法树上的类型、函数、作用域等语义检查。

实验 4 是后端：把合法程序翻译成虚拟机指令，让模拟器真正执行并输出结果。

## 当前模板编译状态

模板引用旧名类的问题已修复：

- `Compiler` 前端已改接 `SymbolTableXK`、`SymbolCollectorXK`、`SemanticCheckerXK`。
- `CompilerMain` 已改为实例化 `CompilerImplXK`。
- 已新增 `CompilerImplXK`、`CodeGeneratorXK`、`AssemblyBuilderXK`、`VariableSlotXK`、`FunctionCompileContextXK`。

当前实现已覆盖实验 4 模板样例中的整数表达式、变量、`if`、`while`、`read`、`write`、`put`、函数调用和递归，并补充了字符串字面量、数组元素读写、`write = array` / `write = arrayFunction()` 字符串输出、`global` / `local` 作用域切换。

## 汇编和虚拟机模型

模板提供的模拟器支持一组栈式虚拟机指令。代码生成时主要输出文本汇编，每行一条指令：

```text
dstore 1
dstore 2
eval 65537
puti 0
jump 4294967295
```

核心指令类别：

- 数据加载：`dload`、`dlload`、`daload`
- 数据写入：`dwrite`、`dlwrite`、`dawrite`
- 常量入栈：`dstore`
- 算术和比较：`eval`
- 跳转：`jump`、`jal`、`ret`
- 堆分配：`malloc`
- 输入输出：`geti`、`getc`、`gets`、`puti`、`putc`、`puts`
- 程序结束：`jump 0xFFFFFFFF`

## 已新增类和职责

### 1. `CompilerImplXK`

实现模板接口 `Compiler`：

```java
final class CompilerImplXK implements Compiler {
    @Override
    public String compile(InputStream inputStream) throws IOException;
}
```

职责：

- 调用 `Compiler.frontend` 完成 ANTLR 前端解析。
- 调用实验 3 的 `SymbolTableXK`、`SymbolCollectorXK`、`SemanticCheckerXK`。
- 调用 `CodeGeneratorXK` 生成汇编文本。

### 2. `CodeGeneratorXK`

ANTLR visitor，遍历 parse tree 输出汇编：

```java
final class CodeGeneratorXK extends MandrillBaseVisitor<Void> {
    String generateXK(MandrillParser.ProgramContext treeXK);
}
```

职责：

- 先收集函数入口信息，再生成函数体，最后生成全局语句。
- 用 visitor 遍历 parse tree，按语句和表达式输出虚拟机汇编。
- 管理全局变量槽位、函数局部变量槽位、函数入口地址。
- 管理 `if` / `while` 的跳转地址回填。

### 3. `AssemblyBuilderXK`

封装汇编文本输出：

```java
final class AssemblyBuilderXK {
    int emitXK(String opcodeXK, long operandXK);
    int positionXK();
    int addressXK(int instructionIndexXK);
    void patchXK(int indexXK, long operandXK);
    String buildXK();
}
```

职责：

- 追加指令并返回指令下标。
- 把指令下标换算成虚拟机 PC 地址。
- 支持 `if` / `while` 跳转地址回填。

### 4. `VariableSlotXK`

记录变量在虚拟机数据区或栈帧中的位置：

```java
final class VariableSlotXK {
    int indexXK;
    boolean localXK;
    boolean arrayXK;
}
```

`arrayXK` 用于区分整数槽位和数组地址槽位。生成 `write = name` 时，如果当前作用域解析到的槽位是数组，则输出 `puts`；否则输出 `puti`。

### 5. `FunctionCompileContextXK`

管理函数代码生成上下文：

```java
final class FunctionCompileContextXK {
    String nameXK;
    int entryIndexXK;
    VariableSlotXK localXK(String nameXK);
    int localBytesXK();
}
```

职责：

- 记录函数入口指令下标。
- 给参数和局部变量分配局部槽位。
- 计算函数调用时需要申请的局部栈帧字节数。

## 当前代码生成链路

### 1. 编译入口

`CompilerMain` 读取输入 `.mds` 文件，调用 `CompilerImplXK.compile`，然后把返回的汇编字符串写入输出文件。

### 2. 前端检查

`CompilerImplXK` 调用 `Compiler.frontend`：

1. ANTLR Lexer 把源码转成 Token。
2. ANTLR Parser 把 Token 转成 parse tree。
3. `SymbolCollectorXK` 收集函数、全局变量、局部变量。
4. `SemanticCheckerXK` 检查类型、作用域、函数参数、`return`、`break`、`continue` 等规则。

只有前端检查通过后，才进入代码生成。

### 3. 后端生成

`CodeGeneratorXK` 使用栈式虚拟机模型生成指令：

- 表达式先把左右操作数压入操作数栈，再输出 `eval`。
- 变量读取输出 `dload` 或 `dlload`。
- 变量写入输出 `dwrite` 或 `dlwrite`。
- 数组变量槽位里保存堆地址，数组元素访问先计算 `base + index * 4`，再输出 `daload` 或 `dawrite`。
- `a[] = b` 会分配新的数组空间并按 0 结尾逐字复制，避免源数组后续修改影响目标数组。
- 字符串字面量通过 `malloc` 申请堆空间，按 UTF-32 风格写入每个字符，并在末尾写入 0；字符串字面量作为函数参数或普通表达式时同样会生成完整字符串地址。
- `func[]` 会记录为数组返回函数，`write = func()` 时输出 `puts`，整数函数仍输出 `puti`。
- `if` / `while` 先输出占位跳转地址，生成完目标代码后用 `patchXK` 回填。
- `break` 回填到当前循环出口，`continue` 跳回当前循环条件判断位置。
- 函数调用先压入实参，再压入局部栈帧大小，最后输出 `jal`。
- 函数返回先把返回值留在操作数栈，再输出 `ret`。
- 函数体生成前会预扫描顶层变量，避免函数先生成时误把顶层全局变量当成局部变量。

## 分阶段实现策略

### 第一阶段：只支持整数主程序

覆盖样例：

- `01-a+1`
- `02-a+b`
- `03-expr`

需要实现：

- 整数常量
- 变量赋值
- `read`
- `write`
- `put`
- 算术表达式
- 比较表达式
- `while`
- `if`

### 第二阶段：支持函数和递归

覆盖样例：

- `29-recursive-gcd`
- `30-fibonacci`

需要实现：

- 函数定义跳过主流程
- 函数入口地址
- 参数压栈
- `jal`
- `ret`
- 返回值压回调用方
- 局部变量槽位

### 第三阶段：支持数组和字符串

已实现：

- 字符串字面量分配堆内存。
- `a[] = "text"`
- `a[] = b` 数组深拷贝。
- 数组元素读写。
- 字符串字面量作为数组参数，例如 `strcpy("Hello World")`。
- `write = a` 输出字符串。

当前未实现：

- `gets` 动态读入字符串。

## 测试方式

编译并运行实验 4：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
bash main.sh compiler-test.log
```

脚本行为：

1. `CompilerMain` 把 `.mds` 编译成 `data.asm`。
2. `SimulatorMain` 用 `data.asm` 和对应 `.in` 执行。
3. 对比 `data.out` 和 `mandrill-ans/*.ans`。

目标：

- `mandrill-src/01-a+1.mds` 输出 `101`
- `mandrill-src/02-a+b.mds` 输出 `3`
- `mandrill-src/03-expr.mds` 输出复杂表达式结果
- `mandrill-src/26-string.mds` 输出 `Hello World`
- `mandrill-src/27-strcpy.mds` 输出拷贝后的 `Hello World`
- `mandrill-src/28-scope.mds` 验证 `global` / `local` 变量切换
- `mandrill-src/29-recursive-gcd.mds` 输出 `6`
- `mandrill-src/30-fibonacci.mds` 输出 `21`
- `mandrill-src/31-array-return-write.mds` 验证 `write = func[]()`
- `mandrill-src/32-array-param-copy.mds` 验证数组参数拷贝
- `mandrill-src/33-array-return-assign.mds` 验证数组返回值赋给数组变量
- `mandrill-src/34-array-local-return-copy.mds` 验证函数内部局部数组拷贝后返回
- `mandrill-src/35-array-assign-copy.mds` 验证 `a[] = b` 是深拷贝
- `mandrill-src/36-string-literal-argument.mds` 验证字符串字面量作为数组参数
- `mandrill-src/37-strcpy-break.mds` 验证 `break` 版本字符串拷贝
- `mandrill-src/38-continue-copy.mds` 验证 `continue` 跳回循环条件

当前结果：

- `bash main.sh compiler-test.log` 通过 16 个本地样例：`01-a+1`、`02-a+b`、`03-expr`、`26-string`、`27-strcpy`、`28-scope`、`29-recursive-gcd`、`30-fibonacci`、`31-array-return-write`、`32-array-param-copy`、`33-array-return-assign`、`34-array-local-return-copy`、`35-array-assign-copy`、`36-string-literal-argument`、`37-strcpy-break`、`38-continue-copy`。

## 后续实现顺序

1. 已修改 `Compiler` 前端复用逻辑，改用实验 3 的 `XK` 语义类。
2. 已修改 `CompilerMain`，实例化 `CompilerImplXK`。
3. 已新增 `AssemblyBuilderXK`，支持输出指令和回填跳转地址。
4. 已新增 `CompilerImplXK` 和 `CodeGeneratorXK`。
5. 已实现整数表达式、变量、读写。
6. 已实现 `if` / `while` 的跳转回填。
7. 已实现函数定义、调用、递归。
8. 已实现字符串常量分配、数组元素读写、字符串输出。
9. 已修复函数先生成导致的全局变量识别问题。
10. 已实现 `func[]` 数组返回值的字符串输出。
11. 已实现数组赋值深拷贝。
12. 已实现字符串字面量作为表达式/参数时的完整堆字符串生成。
13. 已实现 `break` / `continue` 循环跳转。
14. 已通过 `bash main.sh compiler-test.log`。
