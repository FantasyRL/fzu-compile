package cn.edu.fzu.ccds.compilerprinciples.mandrill.compiler;

import java.util.HashMap;
import java.util.Map;

final class FunctionCompileContextXK {
    final String nameXK;
    final Map<String, VariableSlotXK> localsXK = new HashMap<>();
    boolean returnsArrayXK;
    int nextLocalXK = 0;
    int entryIndexXK = -1;

    FunctionCompileContextXK(String nameXK) {
        this.nameXK = nameXK;
    }

    VariableSlotXK localXK(String nameXK) {
        return localsXK.computeIfAbsent(nameXK, keyXK -> new VariableSlotXK(nextLocalXK++, true));
    }

    int localBytesXK() {
        return Math.max(1, nextLocalXK) * 4;
    }
}
