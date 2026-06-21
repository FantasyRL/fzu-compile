package cn.edu.fzu.ccds.compilerprinciples.mandrill.semanticchecker;

import cn.edu.fzu.ccds.compilerprinciples.mandrill.antlr.MandrillBaseVisitor;
import cn.edu.fzu.ccds.compilerprinciples.mandrill.antlr.MandrillParser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

public final class SemanticCheckerXK extends MandrillBaseVisitor<MandrillTypeXK> {
    private final SymbolTableXK tableXK;
    private FunctionSymbolXK currentFunctionXK;
    private int loopDepthXK = 0;

    public SemanticCheckerXK(SymbolTableXK tableXK) {
        this.tableXK = tableXK;
    }

    @Override
    public MandrillTypeXK visitProgram(MandrillParser.ProgramContext ctx) {
        for (MandrillParser.FunctionDefContext functionDefXK : ctx.functionDef()) {
            visit(functionDefXK);
        }
        for (MandrillParser.StatementContext statementXK : ctx.statement()) {
            visit(statementXK);
        }
        return MandrillTypeXK.UNKNOWN;
    }

    @Override
    public MandrillTypeXK visitFunctionDef(MandrillParser.FunctionDefContext ctx) {
        FunctionSymbolXK previousFunctionXK = currentFunctionXK;
        currentFunctionXK = tableXK.findFunctionXK(ctx.Identifier().getText());
        tableXK.enterScopeXK();
        if (ctx.parameterList() != null) {
            List<MandrillParser.ParameterContext> parametersXK = ctx.parameterList().parameter();
            for (int i = 0; i < parametersXK.size(); i++) {
                MandrillParser.ParameterContext parameterXK = parametersXK.get(i);
                tableXK.declareVariableXK(parameterXK.Identifier().getText(), currentFunctionXK.parameterTypesXK.get(i));
            }
        }
        visit(ctx.stmtBlock());
        tableXK.exitScopeXK();
        currentFunctionXK = previousFunctionXK;
        return MandrillTypeXK.UNKNOWN;
    }

    @Override
    public MandrillTypeXK visitStmtBlock(MandrillParser.StmtBlockContext ctx) {
        tableXK.enterScopeXK();
        for (MandrillParser.StatementContext statementXK : ctx.statement()) {
            visit(statementXK);
        }
        tableXK.exitScopeXK();
        return MandrillTypeXK.UNKNOWN;
    }

    @Override
    public MandrillTypeXK visitDeclarationStmt(MandrillParser.DeclarationStmtContext ctx) {
        MandrillTypeXK typeXK = ctx.arraySuffix() == null ? MandrillTypeXK.INT : MandrillTypeXK.ARRAY;
        tableXK.declareVariableXK(ctx.Identifier().getText(), typeXK);
        return MandrillTypeXK.UNKNOWN;
    }

    @Override
    public MandrillTypeXK visitAssignStatement(MandrillParser.AssignStatementContext ctx) {
        MandrillTypeXK valueTypeXK = visit(ctx.rvalue());
        if (ctx.arraySuffix() != null) {
            tableXK.assignVariableXK(ctx.Identifier().getText(), MandrillTypeXK.ARRAY);
            return MandrillTypeXK.UNKNOWN;
        }

        MandrillTypeXK targetTypeXK = visit(ctx.lvalue());
        if (ctx.lvalue() instanceof MandrillParser.PrintIntegerContext) {
            // write 可输出整数，也可输出以 0 结尾的字符串数组。
            return MandrillTypeXK.UNKNOWN;
        }
        if (ctx.lvalue() instanceof MandrillParser.PrintCharContext) {
            // put 只输出单个字符，本语言里字符按 32 位整数处理。
            requireTypeXK(ctx, valueTypeXK, MandrillTypeXK.INT);
            return MandrillTypeXK.UNKNOWN;
        }

        MandrillParser.TargetVariableContext targetXK = (MandrillParser.TargetVariableContext) ctx.lvalue();
        String nameXK = targetXK.Identifier().getText();
        if (targetXK.expression() != null) {
            requireTypeXK(ctx, targetTypeXK, MandrillTypeXK.INT);
            requireTypeXK(ctx, valueTypeXK, MandrillTypeXK.INT);
            return MandrillTypeXK.UNKNOWN;
        }

        MandrillTypeXK oldTypeXK = tableXK.findVariableXK(nameXK);
        if (oldTypeXK != MandrillTypeXK.UNKNOWN && valueTypeXK != MandrillTypeXK.UNKNOWN && oldTypeXK != valueTypeXK) {
            errorXK(ctx, "incompatible assignment");
        }
        tableXK.assignVariableXK(nameXK, valueTypeXK);
        return MandrillTypeXK.UNKNOWN;
    }

    @Override
    public MandrillTypeXK visitRvalue(MandrillParser.RvalueContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public MandrillTypeXK visitJumpStmt(MandrillParser.JumpStmtContext ctx) {
        if ((ctx.Break() != null || ctx.Continue() != null) && loopDepthXK == 0) {
            errorXK(ctx, "break or continue outside loop");
        }
        if (ctx.Return() != null && currentFunctionXK == null) {
            errorXK(ctx, "return outside function");
        }
        if (ctx.Return() != null && ctx.expression() != null && currentFunctionXK != null) {
            MandrillTypeXK actualXK = visit(ctx.expression());
            requireTypeXK(ctx, actualXK, currentFunctionXK.returnTypeXK);
        }
        return MandrillTypeXK.UNKNOWN;
    }

    @Override
    public MandrillTypeXK visitLoopStatement(MandrillParser.LoopStatementContext ctx) {
        requireTypeXK(ctx, visit(ctx.expression()), MandrillTypeXK.INT);
        loopDepthXK++;
        visit(ctx.stmtBlock());
        loopDepthXK--;
        return MandrillTypeXK.UNKNOWN;
    }

    @Override
    public MandrillTypeXK visitConditionStatement(MandrillParser.ConditionStatementContext ctx) {
        requireTypeXK(ctx, visit(ctx.expression()), MandrillTypeXK.INT);
        for (MandrillParser.StmtBlockContext blockXK : ctx.stmtBlock()) {
            visit(blockXK);
        }
        return MandrillTypeXK.UNKNOWN;
    }

    @Override
    public MandrillTypeXK visitIntLiteral(MandrillParser.IntLiteralContext ctx) {
        return MandrillTypeXK.INT;
    }

    @Override
    public MandrillTypeXK visitCharLiteral(MandrillParser.CharLiteralContext ctx) {
        return MandrillTypeXK.INT;
    }

    @Override
    public MandrillTypeXK visitStringLiteral(MandrillParser.StringLiteralContext ctx) {
        return MandrillTypeXK.ARRAY;
    }

    @Override
    public MandrillTypeXK visitInputInt(MandrillParser.InputIntContext ctx) {
        return MandrillTypeXK.INT;
    }

    @Override
    public MandrillTypeXK visitInputChat(MandrillParser.InputChatContext ctx) {
        return MandrillTypeXK.INT;
    }

    @Override
    public MandrillTypeXK visitSourceVariable(MandrillParser.SourceVariableContext ctx) {
        MandrillTypeXK typeXK = tableXK.findVariableXK(ctx.Identifier().getText());
        if (ctx.expression() != null) {
            requireTypeXK(ctx, typeXK, MandrillTypeXK.ARRAY);
            requireTypeXK(ctx, visit(ctx.expression()), MandrillTypeXK.INT);
            return MandrillTypeXK.INT;
        }
        return typeXK;
    }

    @Override
    public MandrillTypeXK visitTargetVariable(MandrillParser.TargetVariableContext ctx) {
        MandrillTypeXK typeXK = tableXK.findVariableXK(ctx.Identifier().getText());
        if (ctx.expression() != null) {
            requireTypeXK(ctx, typeXK, MandrillTypeXK.ARRAY);
            requireTypeXK(ctx, visit(ctx.expression()), MandrillTypeXK.INT);
            return MandrillTypeXK.INT;
        }
        return typeXK;
    }

    @Override
    public MandrillTypeXK visitPrintInteger(MandrillParser.PrintIntegerContext ctx) {
        return MandrillTypeXK.INT;
    }

    @Override
    public MandrillTypeXK visitPrintChar(MandrillParser.PrintCharContext ctx) {
        return MandrillTypeXK.INT;
    }

    @Override
    public MandrillTypeXK visitFunctionCall(MandrillParser.FunctionCallContext ctx) {
        FunctionSymbolXK functionXK = tableXK.findFunctionXK(ctx.Identifier().getText());
        if (functionXK == null) {
            return MandrillTypeXK.UNKNOWN;
        }
        List<MandrillTypeXK> actualTypesXK = new ArrayList<>();
        if (ctx.argumentList() != null) {
            for (MandrillParser.ExpressionContext expressionXK : ctx.argumentList().expression()) {
                actualTypesXK.add(visit(expressionXK));
            }
        }
        if (actualTypesXK.size() != functionXK.parameterTypesXK.size()) {
            errorXK(ctx, "wrong argument count");
        }
        for (int i = 0; i < actualTypesXK.size(); i++) {
            requireTypeXK(ctx, actualTypesXK.get(i), functionXK.parameterTypesXK.get(i));
        }
        return functionXK.returnTypeXK;
    }

    @Override
    public MandrillTypeXK visitSubgroupExpression(MandrillParser.SubgroupExpressionContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public MandrillTypeXK visitMulDivModExpression(MandrillParser.MulDivModExpressionContext ctx) {
        return arithmeticXK(ctx, ctx.expression(0), ctx.expression(1));
    }

    @Override
    public MandrillTypeXK visitAddSubExpression(MandrillParser.AddSubExpressionContext ctx) {
        return arithmeticXK(ctx, ctx.expression(0), ctx.expression(1));
    }

    @Override
    public MandrillTypeXK visitComparingExpression(MandrillParser.ComparingExpressionContext ctx) {
        arithmeticXK(ctx, ctx.expression(0), ctx.expression(1));
        return MandrillTypeXK.INT;
    }

    @Override
    public MandrillTypeXK visitEqualityExpression(MandrillParser.EqualityExpressionContext ctx) {
        MandrillTypeXK leftXK = visit(ctx.expression(0));
        MandrillTypeXK rightXK = visit(ctx.expression(1));
        if (leftXK != MandrillTypeXK.UNKNOWN && rightXK != MandrillTypeXK.UNKNOWN && leftXK != rightXK) {
            errorXK(ctx, "incompatible equality operands");
        }
        return MandrillTypeXK.INT;
    }

    private MandrillTypeXK arithmeticXK(ParserRuleContext ctx, MandrillParser.ExpressionContext leftXK, MandrillParser.ExpressionContext rightXK) {
        requireTypeXK(ctx, visit(leftXK), MandrillTypeXK.INT);
        requireTypeXK(ctx, visit(rightXK), MandrillTypeXK.INT);
        return MandrillTypeXK.INT;
    }

    private void requireTypeXK(ParserRuleContext ctx, MandrillTypeXK actualXK, MandrillTypeXK expectedXK) {
        if (actualXK != MandrillTypeXK.UNKNOWN && actualXK != expectedXK) {
            errorXK(ctx, "type mismatch");
        }
    }

    private void errorXK(ParserRuleContext ctx, String messageXK) {
        int lineXK = ctx.getStart().getLine();
        int colXK = ctx.getStart().getCharPositionInLine();
        throw new SemanticException(lineXK, colXK, messageXK);
    }
}
