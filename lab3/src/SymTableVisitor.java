import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.MultiMap;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.TerminalNode;
import symbol.*;
import type.ArrayType;
import type.Error;
import type.FunctionType;
import type.Primitive;
import type.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SymTableVisitor extends SysYParserBaseVisitor<Type> {

    GlobalScope globalScope;

    Scope currentScope;

    int localScopeCounter;

    String localScopeNamePrefix = "local";


    public static Map<Symbol, List<Pair<Integer, Integer>>> locationMappings = new LinkedHashMap<>();


    @Override
    public Type visitProgram(SysYParser.ProgramContext ctx) {

        globalScope = new GlobalScope();

        currentScope = globalScope;

        return visitChildren(ctx);
    }

    @Override
    public Type visitFuncDef(SysYParser.FuncDefContext ctx) {
        int lineNumber = ctx.getStart().getLine();

        Type returnType = globalScope.resolve(ctx.funcType().getText()).getType();

        String funcName = ctx.IDENT().getText();

        // 检查到函数重定义
        if (globalScope.resolve(funcName) != null) {
            System.err.printf(Error.errorMessageTemplate, Error.ErrorType.redefinedFunction.ordinal() + 1, lineNumber, "function redefined.");
            return new Error();
        }

        List<Type> paramsType = new ArrayList<>();
        List<VariableSymbol> paramSymbols = new ArrayList<>();

        if (ctx.funcFParams() != null) {
            List<SysYParser.FuncFParamContext> params = ctx.funcFParams().funcFParam();

            for (SysYParser.FuncFParamContext param : params) {
                Type paramType = globalScope.resolve(param.bType().getText()).getType();
                String paramName = param.IDENT().getText();
                if (param.R_BRACKT() == null || param.R_BRACKT().isEmpty()) {
                    VariableSymbol paramSymbol = new VariableSymbol(paramName, paramType);
                    paramSymbols.add(paramSymbol);

                    if (locationMappings.get(paramSymbol) == null) {
                        locationMappings.put(paramSymbol, new ArrayList<>());
                    }

                    locationMappings.get(paramSymbol).add(new Pair<>(param.IDENT().getSymbol().getLine(), param.IDENT().getSymbol().getCharPositionInLine()));

                } else {
                    Type currentType = paramType;
                    int dimension = param.R_BRACKT().size();
                    for (int i = 0; i < dimension; i ++) {
                        ArrayType subType = new ArrayType(0, currentType);
                        currentType = subType;
                    }
                    VariableSymbol paramSymbol = new VariableSymbol(paramName, currentType);
                    paramSymbols.add(paramSymbol);

                    if (locationMappings.get(paramSymbol) == null) {
                        locationMappings.put(paramSymbol, new ArrayList<>());
                    }

                    locationMappings.get(paramSymbol).add(new Pair<>(param.IDENT().getSymbol().getLine(), param.IDENT().getSymbol().getCharPositionInLine()));
                }
            }
        }

        FunctionType functionType = new FunctionType(returnType, paramsType);

        FunctionSymbol funcSymbol = new FunctionSymbol(funcName, currentScope, functionType);

        // function should be global

        globalScope.define(funcSymbol);

        if (locationMappings.get(funcSymbol) == null) {
            locationMappings.put(funcSymbol, new ArrayList<>());
        }

        locationMappings.get(funcSymbol).add(new Pair<>(ctx.IDENT().getSymbol().getLine(), ctx.IDENT().getSymbol().getCharPositionInLine()));

        currentScope = funcSymbol;

        for (Symbol param : paramSymbols) {
            if (checkVariableRedefined(param.getName())) {
                System.err.printf(Error.errorMessageTemplate, Error.ErrorType.redefinedVariable.ordinal() + 1, lineNumber, "redefined variable " + param.getName());
            } else {
                paramsType.add(param.getType());
                funcSymbol.define(param);
            }
        }

        visitBlock(ctx.block());

        currentScope = currentScope.getEnclosingScope();

        return null;
    }

    @Override
    public Type visitBlock(SysYParser.BlockContext ctx) {

        LocalScope localScope = new LocalScope(localScopeNamePrefix + localScopeCounter, currentScope);

        localScopeCounter ++;

        currentScope = localScope;

        visitChildren(ctx);

        currentScope = currentScope.getEnclosingScope();

        return null;
    }

    @Override
    public Type visitConstDecl(SysYParser.ConstDeclContext ctx) {

        Type type = globalScope.resolve(ctx.bType().getText()).getType();

        List<SysYParser.ConstDefContext> constDefs = ctx.constDef();

        int lineNumber = ctx.getStart().getLine();

        boolean hasError = false;

        for (SysYParser.ConstDefContext constDef : constDefs) {
            String varName = constDef.IDENT().getText();
            Symbol varSymbol = null;

            Type fromChildren = null;

            if (constDef.constInitVal() != null) {
                fromChildren = visit(constDef.constInitVal());
            }

            if (constDef.L_BRACKT() == null || constDef.L_BRACKT().isEmpty()){

                varSymbol = new VariableSymbol(varName, type);

                if (fromChildren != null && !(fromChildren instanceof Error)) {
                    if (!fromChildren.equals(type)) {
                        hasError = true;
                        System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleAssign.ordinal() + 1, lineNumber, "incompatible assign expression.");
                    }
                }

            }else {
                int dimension = constDef.L_BRACKT().size();
                Primitive primitive = (Primitive) type;

                Type currentType = new Primitive(primitive.getType());

                for (int i = 0; i < dimension; i ++) {
                    Type parentArrayType = new ArrayType(0, currentType);
                    currentType = parentArrayType;
                }

                varSymbol = new VariableSymbol(varName, currentType);

                //TODO： type checking here
                if (fromChildren != null && !(fromChildren instanceof Error)) {

                }
            }

            if (checkVariableRedefined(varName)) {
                System.err.printf(Error.errorMessageTemplate, Error.ErrorType.redefinedVariable.ordinal() + 1, lineNumber, "redefined variable " + varName);
                hasError = true;
            }else {
                currentScope.define(varSymbol);

                if (locationMappings.get(varSymbol) == null) {
                    locationMappings.put(varSymbol, new ArrayList<>());
                }

                locationMappings.get(varSymbol).add(new Pair<>(constDef.IDENT().getSymbol().getLine(), constDef.IDENT().getSymbol().getCharPositionInLine()));
            }
        }

        if (hasError) {
            return new Error();
        }

        return null;
    }

    @Override
    public Type visitVarDecl(SysYParser.VarDeclContext ctx) {

        int lineNumber = ctx.getStart().getLine();

        Type type = globalScope.resolve(ctx.bType().getText()).getType();

        List<SysYParser.VarDefContext> varDefs = ctx.varDef();

        boolean hasError = false;

        for (SysYParser.VarDefContext varDef : varDefs) {
            String varName = varDef.IDENT().getText();
            Symbol varSymbol = null;

            Type fromChildren = null;

            if (varDef.initVal() != null) {
                fromChildren = visit(varDef.initVal());
            }

            if (varDef.L_BRACKT() == null || varDef.L_BRACKT().isEmpty()){
                varSymbol = new VariableSymbol(varName, type);

                if (fromChildren != null && !(fromChildren instanceof Error)) {
                    if (!fromChildren.equals(type)) {
                        hasError = true;
                        System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleAssign.ordinal() + 1, lineNumber, "incompatible assign expression.");
                    }
                }
            }else {
                int dimension = varDef.L_BRACKT().size();
                Primitive primitive = (Primitive) type;

                Type currentType = new Primitive(primitive.getType());

                for (int i = 0; i < dimension; i ++) {
                    Type parentArrayType = new ArrayType(0, currentType);
                    currentType = parentArrayType;
                }

                varSymbol = new VariableSymbol(varName, currentType);

                //TODO： type checking here
                if (fromChildren != null && !(fromChildren instanceof Error)) {

                }
            }

            if (checkVariableRedefined(varName)) {
                System.err.printf(Error.errorMessageTemplate, Error.ErrorType.redefinedVariable.ordinal() + 1, lineNumber, "redefined variable " + varName);
                hasError = true;
            }else {
                currentScope.define(varSymbol);

                if (locationMappings.get(varSymbol) == null) {
                    locationMappings.put(varSymbol, new ArrayList<>());
                }

                locationMappings.get(varSymbol).add(new Pair<>(varDef.IDENT().getSymbol().getLine(), varDef.IDENT().getSymbol().getCharPositionInLine()));

            }

        }

        if (hasError) {
            return new Error();
        }

        return null;
    }


    @Override
    public Type visitFuncCallExp(SysYParser.FuncCallExpContext ctx) {

        Symbol symbol = currentScope.resolve(ctx.IDENT().getText());


        int lineNumber = ctx.IDENT().getSymbol().getLine();

        // 函数未定义
        if (symbol == null) {
            System.err.printf(Error.errorMessageTemplate, Error.ErrorType.undefinedFunction.ordinal() + 1, lineNumber, "function " + ctx.IDENT().getText() + " is undefined.");
            return new Error();
        }

        if (symbol instanceof FunctionSymbol) {
            FunctionSymbol func = (FunctionSymbol) symbol;

            if (locationMappings.get(func) == null) {
                locationMappings.put(func, new ArrayList<>());
            }

            locationMappings.get(func).add(new Pair<>(ctx.IDENT().getSymbol().getLine(), ctx.IDENT().getSymbol().getCharPositionInLine()));

            FunctionType funcType = (FunctionType) func.getType();
            List<Type> paramTypes = funcType.getParamsType();
            if (ctx.funcRParams() == null) {
                // 参数个数不匹配
                if (!paramTypes.isEmpty()) {
                    System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleParameters.ordinal() + 1, lineNumber, "incompatible parameters amount.");
                    return new Error();
                }
            }else {
                List<SysYParser.ParamContext> realParams = ctx.funcRParams().param();
                // 参数个数不匹配
                if (realParams.size() != paramTypes.size()) {
                    System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleParameters.ordinal() + 1, lineNumber, "incompatible parameters amount.");
                    return new Error();
                }

                int argc = realParams.size();

                for (int i = 0; i < argc; i ++) {
                    // 底下就出错了, 默认对的类型

                    Type fromChildren = visit(realParams.get(i).exp());

                    if (fromChildren instanceof Error) {
                        continue;
                    }
                    Type expectedType = paramTypes.get(i);
                    // 类型不匹配
                    if (!expectedType.equals(fromChildren)) {
                        System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleParameters.ordinal() + 1, lineNumber, "incompatible parameters type");
                        return new Error();
                    }
                }
            }

            // 类型检查都通过了
            return funcType.getReturnType();
        }

        // 对不是函数的变量使用调用运算符
        System.err.printf(Error.errorMessageTemplate, Error.ErrorType.variableNotCallable.ordinal() + 1, lineNumber, "variable not callable");

        return new Error();
    }

    @Override
    public Type visitTerminal(TerminalNode node) {

        int lineNumber = node.getSymbol().getLine();

        if (node.getSymbol().getType() == SysYParser.IDENT) {
            String symbolName = node.getText();

            Symbol symbol = currentScope.resolve(symbolName);

            if (symbol == null) {
                System.err.printf(Error.errorMessageTemplate, Error.ErrorType.undeclaredVariable.ordinal() + 1, lineNumber, "variable " + node.getText() + " is undefined.");
                return new Error();
            }

            if (locationMappings.get(symbol) == null) {
                locationMappings.put(symbol, new ArrayList<>());
            }

            locationMappings.get(symbol).add(new Pair<>(node.getSymbol().getLine(), node.getSymbol().getCharPositionInLine()));

            return symbol.getType();
        }

        if (node.getSymbol().getType() == SysYParser.INTEGR_CONST) {
            return new Primitive(Primitive.PrimitiveType.INT);
        }

        return null;
    }

    @Override
    public Type visitReturnStmt(SysYParser.ReturnStmtContext ctx) {

        int lineNumber = ctx.getStart().getLine();

        Type fromChildren = visit(ctx.exp());

        if (!(fromChildren instanceof Error)) {
            Scope scope = currentScope;

            while (scope != null && !(scope instanceof FunctionSymbol)) {
                scope = scope.getEnclosingScope();
            }

            FunctionSymbol functionSymbol = (FunctionSymbol) scope;

            Type returnType = ((FunctionType) functionSymbol.getType()).getReturnType();

            if (!returnType.equals(fromChildren)) {
                System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleReturnValue.ordinal() + 1, lineNumber, "incompatible return value type.");
                return new Error();
            }
        }

        return null;
    }


    @Override
    public Type visitAssignStmt(SysYParser.AssignStmtContext ctx) {

        int lineNumber = ctx.getStart().getLine();

        Type fromLVal = visitLVal(ctx.lVal());

        Type fromRVal = visit(ctx.exp());

        if (!(fromLVal instanceof Error)) {
            if (fromLVal instanceof FunctionType) {
                System.err.printf(Error.errorMessageTemplate, Error.ErrorType.assignToFunction.ordinal() + 1, lineNumber, "left hand side of the assign statement must be variable.");
                return new Error();
            }
        }

        if (!(fromLVal instanceof Error) && !(fromRVal instanceof Error)) {
            if (!fromLVal.equals(fromRVal)) {
                System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleAssign.ordinal() + 1, lineNumber, "incompatible assign expression.");
                return new Error();
            }
        }

        return null;
    }

    @Override
    public Type visitAddExp(SysYParser.AddExpContext ctx) {

        int lineNumber = ctx.getStart().getLine();

        Type left = visit(ctx.exp(0));

        Type right = visit(ctx.exp(1));

        if (left instanceof Error || right instanceof Error) {
            return new Error();
        }

        if (!left.equals(right) || !(left instanceof Primitive)) {
            System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleOperation.ordinal() + 1, lineNumber, "incompatible type operands.");
            return new Error();
        }

        return left;
    }

    @Override
    public Type visitMulExp(SysYParser.MulExpContext ctx) {

        int lineNumber = ctx.getStart().getLine();

        Type left = visit(ctx.exp(0));

        Type right = visit(ctx.exp(1));

        if (left instanceof Error || right instanceof Error) {
            return new Error();
        }

        if (!left.equals(right) || !(left instanceof Primitive)) {
            System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleOperation.ordinal() + 1, lineNumber, "incompatible type operands.");
            return new Error();
        }

        return left;
    }

    @Override
    public Type visitUnaryExp(SysYParser.UnaryExpContext ctx) {
        int lineNumber = ctx.getStart().getLine();

        Type fromChildren = visit(ctx.exp());
        if (fromChildren instanceof Error) {
            return new Error();
        }
        if (fromChildren != null && fromChildren instanceof Primitive){
            return fromChildren;
        }

        System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleOperation.ordinal() + 1, lineNumber, "incompatible operand type.");

        return new Error();
    }

    @Override
    public Type visitParenExp(SysYParser.ParenExpContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public Type visitNumExp(SysYParser.NumExpContext ctx) {
        return visit(ctx.number());
    }

    @Override
    public Type visitNumber(SysYParser.NumberContext ctx) {
        return visit(ctx.INTEGR_CONST());
    }

    @Override
    public Type visitCompareExp(SysYParser.CompareExpContext ctx) {

        int lineNumber = ctx.getStart().getLine();

        Type left = visit(ctx.cond(0));

        Type right = visit(ctx.cond(1));

        if (left instanceof Error || right instanceof Error) {
            return new Error();
        }

        if (!left.equals(right) || !(left instanceof Primitive)) {
            System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleOperation.ordinal() + 1, lineNumber, "incompatible type operands.");
            return new Error();
        }

        return left;
    }

    @Override
    public Type visitRelationExp(SysYParser.RelationExpContext ctx) {

        int lineNumber = ctx.getStart().getLine();

        Type left = visit(ctx.cond(0));

        Type right = visit(ctx.cond(1));

        if (left instanceof Error || right instanceof Error) {
            return new Error();
        }

        if (!left.equals(right) || !(left instanceof Primitive)) {
            System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleOperation.ordinal() + 1, lineNumber, "incompatible type operands.");
            return new Error();
        }

        return left;
    }

    @Override
    public Type visitAndExp(SysYParser.AndExpContext ctx) {

        int lineNumber = ctx.getStart().getLine();

        Type left = visit(ctx.cond(0));

        Type right = visit(ctx.cond(1));

        if (left instanceof Error || right instanceof Error) {
            return new Error();
        }

        if (!left.equals(right) || !(left instanceof Primitive)) {
            System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleOperation.ordinal() + 1, lineNumber, "incompatible type operands.");
            return new Error();
        }

        return left;
    }

    @Override
    public Type visitOrExp(SysYParser.OrExpContext ctx) {

        int lineNumber = ctx.getStart().getLine();

        Type left = visit(ctx.cond(0));

        Type right = visit(ctx.cond(1));

        if (left instanceof Error || right instanceof Error) {
            return new Error();
        }

        if (!left.equals(right) || !(left instanceof Primitive)) {
            System.err.printf(Error.errorMessageTemplate, Error.ErrorType.incompatibleOperation.ordinal() + 1, lineNumber, "incompatible type operands.");
            return new Error();
        }

        return left;
    }


    @Override
    public Type visitLVal(SysYParser.LValContext ctx) {

        int lineNumber = ctx.getStart().getLine();

        List<SysYParser.ExpContext> indexes = ctx.exp();

        if (indexes == null || indexes.isEmpty()) {
            return visitTerminal(ctx.IDENT());
        }

        Type currentType = visitTerminal(ctx.IDENT());

        for (SysYParser.ExpContext index : indexes) {
            // 对函数或者变量使用下标运算符
            if (!(currentType instanceof ArrayType)) {
                System.err.printf(Error.errorMessageTemplate, Error.ErrorType.variableNotAddressable.ordinal() + 1, lineNumber, "only array variable is addressable.");
                return new Error();
            }
            currentType = ((ArrayType) currentType).getElementType();
            visit(index);
        }

        return currentType;
    }

    private boolean checkVariableRedefined(String varName) {
        if (currentScope instanceof GlobalScope) {
            if (globalScope.resolve(varName) != null) {
                return true;
            }
        }

        Scope parentScope = currentScope.getEnclosingScope();

        if (parentScope instanceof FunctionSymbol) {
            parentScope = parentScope.getEnclosingScope();
        }

        if (parentScope == null) {
            parentScope = globalScope;
        }

        if (currentScope.resolve(varName) != parentScope.resolve(varName)) {
            return true;
        }

        return false;
    }
}
