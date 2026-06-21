package cn.edu.fzu.ccds.compilerprinciples.mandrill.semanticchecker;

import cn.edu.fzu.ccds.compilerprinciples.mandrill.antlr.MandrillBaseVisitor;
import cn.edu.fzu.ccds.compilerprinciples.mandrill.antlr.MandrillParser;

import java.util.ArrayList;
import java.util.List;

public final class SymbolCollectorXK extends MandrillBaseVisitor<Void> {
    private final SymbolTableXK tableXK;

    private SymbolCollectorXK(SymbolTableXK tableXK) {
        this.tableXK = tableXK;
    }

    public static void collectXK(MandrillParser.ProgramContext treeXK, SymbolTableXK tableXK) {
        new SymbolCollectorXK(tableXK).visit(treeXK);
    }

    @Override
    public Void visitFunctionDef(MandrillParser.FunctionDefContext ctx) {
        String nameXK = ctx.Identifier().getText();
        MandrillTypeXK returnTypeXK = ctx.arraySuffix() == null ? MandrillTypeXK.INT : MandrillTypeXK.ARRAY;
        List<MandrillTypeXK> parameterTypesXK = new ArrayList<>();
        if (ctx.parameterList() != null) {
            for (MandrillParser.ParameterContext parameterXK : ctx.parameterList().parameter()) {
                parameterTypesXK.add(parameterXK.arraySuffix() == null ? MandrillTypeXK.INT : MandrillTypeXK.ARRAY);
            }
        }
        tableXK.defineFunctionXK(new FunctionSymbolXK(nameXK, returnTypeXK, parameterTypesXK));
        return null;
    }
}
