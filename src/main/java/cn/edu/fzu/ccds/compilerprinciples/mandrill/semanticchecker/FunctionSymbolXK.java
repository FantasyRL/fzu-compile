package cn.edu.fzu.ccds.compilerprinciples.mandrill.semanticchecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class FunctionSymbolXK {
    final String nameXK;
    final MandrillTypeXK returnTypeXK;
    final List<MandrillTypeXK> parameterTypesXK;

    FunctionSymbolXK(String nameXK, MandrillTypeXK returnTypeXK, List<MandrillTypeXK> parameterTypesXK) {
        this.nameXK = nameXK;
        this.returnTypeXK = returnTypeXK;
        this.parameterTypesXK = Collections.unmodifiableList(new ArrayList<>(parameterTypesXK));
    }
}
