package symbol;

import org.bytedeco.llvm.LLVM.LLVMValueRef;

public interface Scope {
    void define(String name, LLVMValueRef value);

    LLVMValueRef resolve(String name);

    Scope getEnclosingScope();
}
