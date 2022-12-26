package type;


import java.util.List;

public class FunctionType implements Type {
    Type returnType;

    List<Type> paramsType;

    public FunctionType(Type returnType, List<Type> paramsType) {
        this.returnType = returnType;
        this.paramsType = paramsType;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Type> getParamsType() {
        return paramsType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public void setParamsType(List<Type> paramsType) {
        this.paramsType = paramsType;
    }
}
