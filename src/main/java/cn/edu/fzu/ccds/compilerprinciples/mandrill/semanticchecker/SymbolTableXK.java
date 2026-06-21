package cn.edu.fzu.ccds.compilerprinciples.mandrill.semanticchecker;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public final class SymbolTableXK {
    private final Map<String, FunctionSymbolXK> functionsXK = new HashMap<>();
    private final ArrayDeque<ScopeXK> scopesXK = new ArrayDeque<>();

    public SymbolTableXK() {
        enterScopeXK();
    }

    void enterScopeXK() {
        scopesXK.push(new ScopeXK());
    }

    void exitScopeXK() {
        if (scopesXK.size() > 1) {
            scopesXK.pop();
        }
    }

    void defineFunctionXK(FunctionSymbolXK functionXK) {
        functionsXK.put(functionXK.nameXK, functionXK);
    }

    FunctionSymbolXK findFunctionXK(String nameXK) {
        return functionsXK.get(nameXK);
    }

    void declareVariableXK(String nameXK, MandrillTypeXK typeXK) {
        scopesXK.peek().putXK(nameXK, typeXK);
    }

    MandrillTypeXK findVariableXK(String nameXK) {
        for (ScopeXK scopeXK : scopesXK) {
            MandrillTypeXK typeXK = scopeXK.getLocalXK(nameXK);
            if (typeXK != null) {
                return typeXK;
            }
        }
        return MandrillTypeXK.UNKNOWN;
    }

    void assignVariableXK(String nameXK, MandrillTypeXK typeXK) {
        for (ScopeXK scopeXK : scopesXK) {
            if (scopeXK.containsLocalXK(nameXK)) {
                scopeXK.putXK(nameXK, typeXK);
                return;
            }
        }
        declareVariableXK(nameXK, typeXK);
    }
}
