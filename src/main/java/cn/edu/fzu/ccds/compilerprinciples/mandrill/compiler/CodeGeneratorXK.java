package cn.edu.fzu.ccds.compilerprinciples.mandrill.compiler;

import cn.edu.fzu.ccds.compilerprinciples.mandrill.antlr.MandrillBaseVisitor;
import cn.edu.fzu.ccds.compilerprinciples.mandrill.antlr.MandrillParser;
import cn.edu.fzu.ccds.compilerprinciples.mandrill.simulator.Constants;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CodeGeneratorXK extends MandrillBaseVisitor<Void> {
    private static final long HALT_XK = 0xFFFFFFFFL;
    private static final int EMPTY_STRING_CAPACITY_XK = 256;

    private final AssemblyBuilderXK outXK = new AssemblyBuilderXK();
    private final Map<String, VariableSlotXK> globalsXK = new HashMap<>();
    private final Map<String, FunctionCompileContextXK> functionsXK = new HashMap<>();
    private final Deque<Map<String, Boolean>> scopeModesXK = new ArrayDeque<>();
    private final Deque<LoopContextXK> loopsXK = new ArrayDeque<>();
    private FunctionCompileContextXK currentFunctionXK;
    private int nextGlobalXK = 0;
    private int nextTempGlobalXK = 10000;

    private static final class LoopContextXK {
        final long continueAddressXK;
        final List<Integer> breakJumpsXK = new ArrayList<>();

        LoopContextXK(long continueAddressXK) {
            this.continueAddressXK = continueAddressXK;
        }
    }

    String generateXK(MandrillParser.ProgramContext treeXK) {
        collectGlobalsXK(treeXK);
        collectFunctionsXK(treeXK);

        int jumpMainXK = outXK.emitXK("jump", 0);
        for (MandrillParser.FunctionDefContext functionXK : treeXK.functionDef()) {
            generateFunctionXK(functionXK);
        }

        outXK.patchXK(jumpMainXK, outXK.addressXK(outXK.positionXK()));
        for (MandrillParser.StatementContext statementXK : treeXK.statement()) {
            visit(statementXK);
        }
        outXK.emitXK("jump", HALT_XK);
        return outXK.buildXK();
    }

    private void collectFunctionsXK(MandrillParser.ProgramContext treeXK) {
        for (MandrillParser.FunctionDefContext functionXK : treeXK.functionDef()) {
            FunctionCompileContextXK contextXK = new FunctionCompileContextXK(functionXK.Identifier().getText());
            contextXK.returnsArrayXK = functionXK.arraySuffix() != null;
            functionsXK.put(functionXK.Identifier().getText(), contextXK);
        }
    }

    private void collectGlobalsXK(MandrillParser.ProgramContext treeXK) {
        for (MandrillParser.StatementContext statementXK : treeXK.statement()) {
            if (statementXK.assignStatement() != null) {
                MandrillParser.AssignStatementContext assignXK = statementXK.assignStatement();
                if (assignXK.arraySuffix() != null) {
                    globalSlotXK(assignXK.Identifier().getText()).arrayXK = true;
                } else if (assignXK.lvalue() instanceof MandrillParser.TargetVariableContext targetXK) {
                    globalSlotXK(targetXK.Identifier().getText());
                }
            } else if (statementXK.declarationStmt() != null) {
                MandrillParser.DeclarationStmtContext declarationXK = statementXK.declarationStmt();
                if (declarationXK.arraySuffix() != null) {
                    globalSlotXK(declarationXK.Identifier().getText()).arrayXK = true;
                } else {
                    globalSlotXK(declarationXK.Identifier().getText());
                }
            }
        }
    }

    private void generateFunctionXK(MandrillParser.FunctionDefContext ctx) {
        FunctionCompileContextXK previousXK = currentFunctionXK;
        currentFunctionXK = functionsXK.get(ctx.Identifier().getText());
        currentFunctionXK.entryIndexXK = outXK.positionXK();

        if (ctx.parameterList() != null) {
            for (MandrillParser.ParameterContext parameterXK : ctx.parameterList().parameter()) {
                currentFunctionXK.localXK(parameterXK.Identifier().getText()).arrayXK = parameterXK.arraySuffix() != null;
            }
            for (int i = ctx.parameterList().parameter().size() - 1; i >= 0; i--) {
                outXK.emitXK("dlwrite", i);
            }
        }

        visit(ctx.stmtBlock());
        outXK.emitXK("dstore", 0);
        outXK.emitXK("ret", 0);
        currentFunctionXK = previousXK;
    }

    @Override
    public Void visitStmtBlock(MandrillParser.StmtBlockContext ctx) {
        scopeModesXK.push(new HashMap<>());
        for (MandrillParser.StatementContext statementXK : ctx.statement()) {
            visit(statementXK);
        }
        scopeModesXK.pop();
        return null;
    }

    @Override
    public Void visitDeclarationStmt(MandrillParser.DeclarationStmtContext ctx) {
        String nameXK = ctx.Identifier().getText();
        if (currentFunctionXK != null && ctx.Global() != null) {
            globalSlotXK(nameXK).arrayXK = ctx.arraySuffix() != null;
            scopeModesXK.peek().put(nameXK, false);
            return null;
        }
        if (currentFunctionXK != null && ctx.Local() != null) {
            currentFunctionXK.localXK(nameXK).arrayXK = ctx.arraySuffix() != null;
            scopeModesXK.peek().put(nameXK, true);
            return null;
        }
        globalSlotXK(nameXK).arrayXK = ctx.arraySuffix() != null;
        return null;
    }

    @Override
    public Void visitAssignStatement(MandrillParser.AssignStatementContext ctx) {
        if (ctx.arraySuffix() != null) {
            slotXK(ctx.Identifier().getText()).arrayXK = true;
            if (ctx.rvalue().expression() instanceof MandrillParser.StringLiteralContext stringXK) {
                emitStringAssignmentXK(ctx.Identifier().getText(), stringXK.StringConstant().getText());
                return null;
            }
            if (isArrayExpressionXK(ctx.rvalue().expression())) {
                copyArrayValueXK(ctx.Identifier().getText(), ctx.rvalue().expression());
                return null;
            }
            visit(ctx.rvalue());
            storeXK(ctx.Identifier().getText());
            return null;
        }
        if (ctx.lvalue() instanceof MandrillParser.TargetVariableContext targetXK && targetXK.expression() != null) {
            visit(ctx.rvalue());
            arrayAddressXK(targetXK.Identifier().getText(), targetXK.expression());
            outXK.emitXK("dawrite", 0);
            return null;
        }
        visit(ctx.rvalue());
        if (ctx.lvalue() instanceof MandrillParser.PrintIntegerContext) {
            outXK.emitXK(printsStringXK(ctx.rvalue().expression()) ? "puts" : "puti", 0);
            return null;
        }
        if (ctx.lvalue() instanceof MandrillParser.PrintCharContext) {
            outXK.emitXK("putc", 0);
            return null;
        }
        MandrillParser.TargetVariableContext targetXK = (MandrillParser.TargetVariableContext) ctx.lvalue();
        storeXK(targetXK.Identifier().getText());
        return null;
    }

    @Override
    public Void visitLoopStatement(MandrillParser.LoopStatementContext ctx) {
        int conditionIndexXK = outXK.positionXK();
        int falseTargetXK = outXK.emitXK("dstore", 0);
        int trueTargetXK = outXK.emitXK("dstore", 0);
        visit(ctx.expression());
        outXK.emitXK("eval", Constants.EVAL_CONDITION);
        outXK.patchXK(trueTargetXK, outXK.addressXK(outXK.positionXK()));
        LoopContextXK loopXK = new LoopContextXK(outXK.addressXK(conditionIndexXK));
        loopsXK.push(loopXK);
        visit(ctx.stmtBlock());
        loopsXK.pop();
        outXK.emitXK("jump", outXK.addressXK(conditionIndexXK));
        long loopEndXK = outXK.addressXK(outXK.positionXK());
        outXK.patchXK(falseTargetXK, loopEndXK);
        for (Integer breakJumpXK : loopXK.breakJumpsXK) {
            outXK.patchXK(breakJumpXK, loopEndXK);
        }
        return null;
    }

    @Override
    public Void visitConditionStatement(MandrillParser.ConditionStatementContext ctx) {
        int falseTargetXK = outXK.emitXK("dstore", 0);
        int trueTargetXK = outXK.emitXK("dstore", 0);
        visit(ctx.expression());
        outXK.emitXK("eval", Constants.EVAL_CONDITION);
        outXK.patchXK(trueTargetXK, outXK.addressXK(outXK.positionXK()));
        visit(ctx.stmtBlock(0));
        int jumpEndXK = outXK.emitXK("jump", 0);
        outXK.patchXK(falseTargetXK, outXK.addressXK(outXK.positionXK()));
        if (ctx.stmtBlock().size() > 1) {
            visit(ctx.stmtBlock(1));
        }
        outXK.patchXK(jumpEndXK, outXK.addressXK(outXK.positionXK()));
        return null;
    }

    @Override
    public Void visitJumpStmt(MandrillParser.JumpStmtContext ctx) {
        if (ctx.Break() != null) {
            loopsXK.peek().breakJumpsXK.add(outXK.emitXK("jump", 0));
            return null;
        }
        if (ctx.Continue() != null) {
            outXK.emitXK("jump", loopsXK.peek().continueAddressXK);
            return null;
        }
        if (ctx.Return() != null) {
            visit(ctx.expression());
            outXK.emitXK("ret", 0);
        }
        return null;
    }

    @Override
    public Void visitRvalue(MandrillParser.RvalueContext ctx) {
        visit(ctx.expression());
        return null;
    }

    @Override
    public Void visitIntLiteral(MandrillParser.IntLiteralContext ctx) {
        outXK.emitXK("dstore", Long.parseLong(ctx.IntegerConstant().getText()));
        return null;
    }

    @Override
    public Void visitCharLiteral(MandrillParser.CharLiteralContext ctx) {
        outXK.emitXK("dstore", charValueXK(ctx.CharacterConstant().getText()));
        return null;
    }

    @Override
    public Void visitInputInt(MandrillParser.InputIntContext ctx) {
        outXK.emitXK("geti", 0);
        return null;
    }

    @Override
    public Void visitInputChat(MandrillParser.InputChatContext ctx) {
        outXK.emitXK("getc", 0);
        return null;
    }

    @Override
    public Void visitStringLiteral(MandrillParser.StringLiteralContext ctx) {
        emitStringLiteralAddressXK(ctx.StringConstant().getText());
        return null;
    }

    private void emitStringLiteralAddressXK(String textXK) {
        int tempSlotXK = nextTempGlobalXK++;
        int[] charsXK = stringValueXK(textXK).codePoints().toArray();
        int capacityXK = charsXK.length == 0 ? EMPTY_STRING_CAPACITY_XK : charsXK.length + 1;
        outXK.emitXK("dstore", (long) capacityXK * 4L);
        outXK.emitXK("malloc", 0);
        outXK.emitXK("dwrite", tempSlotXK);
        for (int i = 0; i < charsXK.length; i++) {
            outXK.emitXK("dstore", charsXK[i]);
            outXK.emitXK("dload", tempSlotXK);
            outXK.emitXK("dstore", i * 4L);
            outXK.emitXK("eval", Constants.EVAL_ADD);
            outXK.emitXK("dawrite", 0);
        }
        outXK.emitXK("dstore", 0);
        outXK.emitXK("dload", tempSlotXK);
        outXK.emitXK("dstore", (long) charsXK.length * 4L);
        outXK.emitXK("eval", Constants.EVAL_ADD);
        outXK.emitXK("dawrite", 0);
        outXK.emitXK("dload", tempSlotXK);
    }

    private void emitStringAssignmentXK(String nameXK, String textXK) {
        int[] charsXK = stringValueXK(textXK).codePoints().toArray();
        int capacityXK = charsXK.length == 0 ? EMPTY_STRING_CAPACITY_XK : charsXK.length + 1;
        outXK.emitXK("dstore", (long) capacityXK * 4L);
        outXK.emitXK("malloc", 0);
        storeXK(nameXK);
        for (int i = 0; i < charsXK.length; i++) {
            outXK.emitXK("dstore", charsXK[i]);
            loadXK(nameXK);
            outXK.emitXK("dstore", i * 4L);
            outXK.emitXK("eval", Constants.EVAL_ADD);
            outXK.emitXK("dawrite", 0);
        }
        outXK.emitXK("dstore", 0);
        loadXK(nameXK);
        outXK.emitXK("dstore", (long) charsXK.length * 4L);
        outXK.emitXK("eval", Constants.EVAL_ADD);
        outXK.emitXK("dawrite", 0);
    }

    private void copyArrayValueXK(String targetNameXK, MandrillParser.ExpressionContext sourceXK) {
        int indexSlotXK = nextTempGlobalXK++;
        outXK.emitXK("dstore", (long) EMPTY_STRING_CAPACITY_XK * 4L);
        outXK.emitXK("malloc", 0);
        storeXK(targetNameXK);
        outXK.emitXK("dstore", 0);
        outXK.emitXK("dwrite", indexSlotXK);

        int loopStartXK = outXK.positionXK();
        int falseTargetXK = outXK.emitXK("dstore", 0);
        int trueTargetXK = outXK.emitXK("dstore", 0);
        emitArrayElementLoadXK(sourceXK, indexSlotXK);
        outXK.emitXK("dstore", 0);
        outXK.emitXK("eval", Constants.EVAL_NOT_EQUAL);
        outXK.emitXK("eval", Constants.EVAL_CONDITION);

        outXK.patchXK(trueTargetXK, outXK.addressXK(outXK.positionXK()));
        emitArrayElementLoadXK(sourceXK, indexSlotXK);
        loadXK(targetNameXK);
        outXK.emitXK("dload", indexSlotXK);
        outXK.emitXK("dstore", 4);
        outXK.emitXK("eval", Constants.EVAL_MUL);
        outXK.emitXK("eval", Constants.EVAL_ADD);
        outXK.emitXK("dawrite", 0);
        outXK.emitXK("dload", indexSlotXK);
        outXK.emitXK("dstore", 1);
        outXK.emitXK("eval", Constants.EVAL_ADD);
        outXK.emitXK("dwrite", indexSlotXK);
        outXK.emitXK("jump", outXK.addressXK(loopStartXK));

        outXK.patchXK(falseTargetXK, outXK.addressXK(outXK.positionXK()));
        outXK.emitXK("dstore", 0);
        loadXK(targetNameXK);
        outXK.emitXK("dload", indexSlotXK);
        outXK.emitXK("dstore", 4);
        outXK.emitXK("eval", Constants.EVAL_MUL);
        outXK.emitXK("eval", Constants.EVAL_ADD);
        outXK.emitXK("dawrite", 0);
    }

    private void emitArrayElementLoadXK(MandrillParser.ExpressionContext sourceXK, int indexSlotXK) {
        visit(sourceXK);
        outXK.emitXK("dload", indexSlotXK);
        outXK.emitXK("dstore", 4);
        outXK.emitXK("eval", Constants.EVAL_MUL);
        outXK.emitXK("eval", Constants.EVAL_ADD);
        outXK.emitXK("daload", 0);
    }

    @Override
    public Void visitSourceVariable(MandrillParser.SourceVariableContext ctx) {
        if (ctx.expression() != null) {
            arrayAddressXK(ctx.Identifier().getText(), ctx.expression());
            outXK.emitXK("daload", 0);
            return null;
        }
        loadXK(ctx.Identifier().getText());
        return null;
    }

    @Override
    public Void visitFunctionCall(MandrillParser.FunctionCallContext ctx) {
        if (ctx.argumentList() != null) {
            for (MandrillParser.ExpressionContext argumentXK : ctx.argumentList().expression()) {
                visit(argumentXK);
            }
        }
        FunctionCompileContextXK functionXK = functionsXK.get(ctx.Identifier().getText());
        outXK.emitXK("dstore", functionXK.localBytesXK());
        outXK.emitXK("jal", outXK.addressXK(functionXK.entryIndexXK));
        return null;
    }

    @Override
    public Void visitSubgroupExpression(MandrillParser.SubgroupExpressionContext ctx) {
        visit(ctx.expression());
        return null;
    }

    @Override
    public Void visitMulDivModExpression(MandrillParser.MulDivModExpressionContext ctx) {
        visit(ctx.expression(0));
        visit(ctx.expression(1));
        outXK.emitXK("eval", switch (ctx.op.getText()) {
            case "*" -> Constants.EVAL_MUL;
            case "/" -> Constants.EVAL_DIV;
            default -> Constants.EVAL_MOD;
        });
        return null;
    }

    @Override
    public Void visitAddSubExpression(MandrillParser.AddSubExpressionContext ctx) {
        visit(ctx.expression(0));
        visit(ctx.expression(1));
        outXK.emitXK("eval", ctx.op.getText().equals("+") ? Constants.EVAL_ADD : Constants.EVAL_MINUS);
        return null;
    }

    @Override
    public Void visitComparingExpression(MandrillParser.ComparingExpressionContext ctx) {
        visit(ctx.expression(0));
        visit(ctx.expression(1));
        outXK.emitXK("eval", switch (ctx.op.getText()) {
            case ">" -> Constants.EVAL_GREATER;
            case "<" -> Constants.EVAL_LESS;
            case ">=" -> Constants.EVAL_GREATER_OR_EQUAL;
            default -> Constants.EVAL_LESS_OR_EQUAL;
        });
        return null;
    }

    @Override
    public Void visitEqualityExpression(MandrillParser.EqualityExpressionContext ctx) {
        visit(ctx.expression(0));
        visit(ctx.expression(1));
        outXK.emitXK("eval", ctx.op.getText().equals("==") ? Constants.EVAL_EQUAL : Constants.EVAL_NOT_EQUAL);
        return null;
    }

    private VariableSlotXK globalSlotXK(String nameXK) {
        return globalsXK.computeIfAbsent(nameXK, keyXK -> new VariableSlotXK(nextGlobalXK++, false));
    }

    private VariableSlotXK slotXK(String nameXK) {
        Boolean localModeXK = currentScopeModeXK(nameXK);
        if (localModeXK != null) {
            return localModeXK ? currentFunctionXK.localXK(nameXK) : globalSlotXK(nameXK);
        }
        if (currentFunctionXK != null) {
            if (currentFunctionXK.localsXK.containsKey(nameXK)) {
                return currentFunctionXK.localXK(nameXK);
            }
            if (globalsXK.containsKey(nameXK)) {
                return globalSlotXK(nameXK);
            }
            return currentFunctionXK.localXK(nameXK);
        }
        return globalSlotXK(nameXK);
    }

    private Boolean currentScopeModeXK(String nameXK) {
        for (Map<String, Boolean> modesXK : scopeModesXK) {
            if (modesXK.containsKey(nameXK)) {
                return modesXK.get(nameXK);
            }
        }
        return null;
    }

    private void loadXK(String nameXK) {
        VariableSlotXK slotXK = slotXK(nameXK);
        outXK.emitXK(slotXK.localXK ? "dlload" : "dload", slotXK.indexXK);
    }

    private void storeXK(String nameXK) {
        VariableSlotXK slotXK = slotXK(nameXK);
        outXK.emitXK(slotXK.localXK ? "dlwrite" : "dwrite", slotXK.indexXK);
    }

    private void arrayAddressXK(String nameXK, MandrillParser.ExpressionContext indexXK) {
        loadXK(nameXK);
        visit(indexXK);
        outXK.emitXK("dstore", 4);
        outXK.emitXK("eval", Constants.EVAL_MUL);
        outXK.emitXK("eval", Constants.EVAL_ADD);
    }

    private boolean printsStringXK(MandrillParser.ExpressionContext expressionXK) {
        return isArrayExpressionXK(expressionXK);
    }

    private boolean isArrayExpressionXK(MandrillParser.ExpressionContext expressionXK) {
        if (expressionXK instanceof MandrillParser.SourceVariableContext sourceXK && sourceXK.expression() == null) {
            return slotXK(sourceXK.Identifier().getText()).arrayXK;
        }
        if (expressionXK instanceof MandrillParser.FunctionCallContext callXK) {
            FunctionCompileContextXK functionXK = functionsXK.get(callXK.Identifier().getText());
            return functionXK != null && functionXK.returnsArrayXK;
        }
        if (expressionXK instanceof MandrillParser.StringLiteralContext) {
            return true;
        }
        return false;
    }

    private long charValueXK(String textXK) {
        String bodyXK = textXK.substring(1, textXK.length() - 1);
        if (bodyXK.equals("\\n")) {
            return '\n';
        }
        if (bodyXK.equals("\\\\")) {
            return '\\';
        }
        if (bodyXK.equals("\\'")) {
            return '\'';
        }
        return bodyXK.codePointAt(0);
    }

    private String stringValueXK(String textXK) {
        String bodyXK = textXK.substring(1, textXK.length() - 1);
        StringBuilder builderXK = new StringBuilder();
        for (int i = 0; i < bodyXK.length(); i++) {
            char chXK = bodyXK.charAt(i);
            if (chXK != '\\' || i + 1 >= bodyXK.length()) {
                builderXK.append(chXK);
                continue;
            }
            char escapedXK = bodyXK.charAt(++i);
            builderXK.append(switch (escapedXK) {
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'b' -> '\b';
                case 'f' -> '\f';
                case '\\' -> '\\';
                case '"' -> '"';
                case '\'' -> '\'';
                default -> escapedXK;
            });
        }
        return builderXK.toString();
    }

}
