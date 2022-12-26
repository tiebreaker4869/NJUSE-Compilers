package symbol;

public class GlobalScope extends BaseScope {
    public GlobalScope() {
        super("global", null);
        define(new PrimitiveSymbol("int"));
        define(new PrimitiveSymbol("float"));
        define(new PrimitiveSymbol("void"));
    }
}
