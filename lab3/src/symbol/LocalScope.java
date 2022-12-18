package symbol;


public class LocalScope extends BaseScope {

    public LocalScope(String name, Scope enclosingScope) {
        super(name, enclosingScope);
    }

}
