package symbol;

import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.HashMap;
import java.util.Map;

public class BaseScope implements Scope {
    /**
     * 符号表, 记录符号名称和符号的对应关系
     */
    private Map<String, LLVMValueRef> valueRefMap;

    /**
     * scope 的唯一标识名称
     */
    private String name;

    /**
     * 当前 scope 的上一级 scope
     */
    private Scope enclosingScope;

    public BaseScope(String name, Scope enclosingScope) {
        this.name = name;
        this.enclosingScope = enclosingScope;
        valueRefMap = new HashMap<>();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Scope getEnclosingScope() {
        return this.enclosingScope;
    }


    @Override
    public void define(String name, LLVMValueRef value) {
        valueRefMap.put(name, value);
    }

    @Override
    public LLVMValueRef resolve(String name) {

        LLVMValueRef findInHere = valueRefMap.get(name);

        if (findInHere != null) {
            return findInHere;
        }

        if (enclosingScope != null) {
            LLVMValueRef findInParent = enclosingScope.resolve(name);

            if (findInParent != null) {
                return findInParent;
            }
        }

        return null;
    }
}
