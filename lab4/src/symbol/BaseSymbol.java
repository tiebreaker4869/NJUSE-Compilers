package symbol;

import type.Type;

public class BaseSymbol implements Symbol {

    Type type;

    String name;

    public BaseSymbol(String name, Type type) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

}
