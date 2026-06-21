package cn.edu.fzu.ccds.compilerprinciples.mandrill.compiler;

import java.io.IOException;
import java.io.InputStream;

public final class CompilerImplXK implements Compiler {
    @Override
    public String compile(InputStream inputStream) throws IOException {
        CompileContext contextXK = Compiler.frontend(inputStream);
        return new CodeGeneratorXK().generateXK(contextXK.tree());
    }
}
