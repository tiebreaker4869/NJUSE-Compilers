package symbol;

import type.FunctionType;
import type.Type;

public class FunctionSymbol extends BaseScope implements Symbol{

    FunctionType type;

    public FunctionSymbol(String name, Scope enclosingScope, FunctionType type) {
        super(name, enclosingScope);
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }
}
