package cn.edu.fzu.ccds.compilerprinciples.mandrill.compiler;

import java.util.ArrayList;
import java.util.List;

final class AssemblyBuilderXK {
    private final List<String> linesXK = new ArrayList<>();

    int emitXK(String opcodeXK, long operandXK) {
        linesXK.add(opcodeXK + " " + operandXK);
        return linesXK.size() - 1;
    }

    int positionXK() {
        return linesXK.size();
    }

    long addressXK(int indexXK) {
        return (long) indexXK * 8L;
    }

    void patchXK(int indexXK, long operandXK) {
        String lineXK = linesXK.get(indexXK);
        String opcodeXK = lineXK.split("\\s+")[0];
        linesXK.set(indexXK, opcodeXK + " " + operandXK);
    }

    String buildXK() {
        StringBuilder builderXK = new StringBuilder();
        for (String lineXK : linesXK) {
            builderXK.append(lineXK).append('\n');
        }
        return builderXK.toString();
    }
}
