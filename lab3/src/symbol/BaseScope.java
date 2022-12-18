package symbol;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope {
    /**
     * 符号表, 记录符号名称和符号的对应关系
     */
    private Map<String, Symbol> symbols;

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
        symbols = new LinkedHashMap<>();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Scope getEnclosingScope() {
        return this.enclosingScope;
    }

    @Override
    public Map<String, Symbol> getSymbols() {
        return symbols;
    }

    @Override
    public void define(Symbol symbol) {
        String symbolName = symbol.getName();
        symbols.put(symbolName, symbol);
//        System.err.println("+ " + symbolName);
    }

    @Override
    public Symbol resolve(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
//            if (!(symbol instanceof PrimitiveSymbol)){
//                System.err.println(this.name + ": " + "*" + name);
//            }
            return symbol;
        }

        if (enclosingScope != null) {
            return enclosingScope.resolve(name);
        }

        return null;
    }
}
