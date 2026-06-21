package cn.edu.fzu.ccds.compilerprinciples.mandrill.semanticchecker;

import java.util.HashMap;
import java.util.Map;

final class ScopeXK {
    private final Map<String, MandrillTypeXK> variablesXK = new HashMap<>();

    boolean containsLocalXK(String nameXK) {
        return variablesXK.containsKey(nameXK);
    }

    MandrillTypeXK getLocalXK(String nameXK) {
        return variablesXK.get(nameXK);
    }

    void putXK(String nameXK, MandrillTypeXK typeXK) {
        variablesXK.put(nameXK, typeXK);
    }
}
