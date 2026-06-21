package cn.edu.fzu.ccds.compilerprinciples.mandrill.compiler;

final class VariableSlotXK {
    final int indexXK;
    final boolean localXK;
    boolean arrayXK;

    VariableSlotXK(int indexXK, boolean localXK) {
        this.indexXK = indexXK;
        this.localXK = localXK;
    }
}
