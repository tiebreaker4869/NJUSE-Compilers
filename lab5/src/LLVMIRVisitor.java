import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import symbol.GlobalScope;
import symbol.LocalScope;
import symbol.Scope;


import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMIRVisitor extends SysYParserBaseVisitor<LLVMValueRef> {

    private LLVMModuleRef module = LLVMModuleCreateWithName("module");

    private LLVMTypeRef i32Type = LLVMInt32Type();

    private LLVMTypeRef voidType = LLVMVoidType();

    private LLVMBuilderRef builder = LLVMCreateBuilder();

    private LLVMValueRef zero = LLVMConstInt(i32Type, 0, 0);

    private GlobalScope globalScope;

    private Scope currentScope;

    private int localCounter;

    private String targetPath;

    private boolean hasReturn;

    public LLVMIRVisitor(String outputPath) {
        this.targetPath = outputPath;
        //初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();

        globalScope = new GlobalScope();

        currentScope = globalScope;
    }

    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {

        visitChildren(ctx);

        BytePointer error = new BytePointer();

        LLVMPrintModuleToFile(module,targetPath, error);

        return null;
    }

    @Override
    public LLVMValueRef visitReturnStmt(SysYParser.ReturnStmtContext ctx) {

        if (ctx.exp() == null) {
            LLVMBuildRetVoid(builder);

            return null;
        }

        LLVMValueRef result = visit(ctx.exp());

        LLVMBuildRet(builder, result);

        hasReturn = true;

        return null;
    }

    @Override
    public LLVMValueRef visitUnaryExp(SysYParser.UnaryExpContext ctx) {
        String op = ctx.unaryOp().getText();

        LLVMValueRef expValue = visit(ctx.exp());

        switch (op) {
            case "+":
                return expValue;
            case "-":
                return LLVMBuildNeg(builder, expValue, "tmp_");
            case "!":
                long value = LLVMConstIntGetZExtValue(expValue);
                if (value == 0) {
                    return LLVMConstInt(i32Type, 1, 0);
                } else {
                    return LLVMConstInt(i32Type, 0, 0);
                }

            default:
                break;
        }

        return null;
    }

    @Override
    public LLVMValueRef visitParenExp(SysYParser.ParenExpContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public LLVMValueRef visitAddExp(SysYParser.AddExpContext ctx) {
        if (ctx.PLUS() != null) {
            return binaryOperation("+", visit(ctx.exp(0)), visit(ctx.exp(1)));
        }

        return binaryOperation("-", visit(ctx.exp(0)), visit(ctx.exp(1)));
    }

    @Override
    public LLVMValueRef visitMulExp(SysYParser.MulExpContext ctx) {
        if (ctx.MUL() != null) {
            return binaryOperation("*", visit(ctx.exp(0)), visit(ctx.exp(1)));
        }

        if (ctx.DIV() != null) {
            return binaryOperation("/", visit(ctx.exp(0)), visit(ctx.exp(1)));
        }

        return binaryOperation("%", visit(ctx.exp(0)), visit(ctx.exp(1)));
    }

    @Override
    public LLVMValueRef visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == SysYParser.INTEGR_CONST) {
            int number = toDecimal(node.getText());
            return LLVMConstInt(i32Type, number, 0);
        }

        return super.visitTerminal(node);
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {

        int paramsCount;

        if (ctx.funcFParams() == null) {
            paramsCount = 0;
        } else {
            paramsCount = ctx.funcFParams().funcFParam().size();
        }

        LLVMTypeRef returnType = getTypeByName(ctx.funcType().getText());

        PointerPointer<Pointer> paramsType = new PointerPointer<>(paramsCount);

        for (int i = 0; i < paramsCount; i ++) {
            paramsType.put(i, i32Type);
        }

        LLVMTypeRef functionType = LLVMFunctionType(returnType, paramsType, paramsCount, 0);

        String functionName = ctx.IDENT().getText();

        LLVMValueRef function = LLVMAddFunction(module, functionName, functionType);

        LLVMBasicBlockRef entryBlock = LLVMAppendBasicBlock(function, functionName + "_entry");

        LLVMPositionBuilderAtEnd(builder, entryBlock);

        globalScope.define(functionName, function);

        LocalScope functionScope = new LocalScope(functionName, currentScope);

        currentScope = functionScope;

        for (int i = 0; i < paramsCount; i ++) {
            SysYParser.FuncFParamContext funcFParamContext = ctx.funcFParams().funcFParam(i);
            String paramName = funcFParamContext.IDENT().getText();
            LLVMTypeRef paramType = i32Type;
            LLVMValueRef varPointer = LLVMBuildAlloca(builder, paramType, "param_" + paramName);
            LLVMValueRef argValue = LLVMGetParam(function, i);
            LLVMBuildStore(builder, argValue, varPointer);
            currentScope.define(paramName, varPointer);
        }

        hasReturn = false;

        visitBlock(ctx.block());

        currentScope = currentScope.getEnclosingScope();

        if (!hasReturn) {
            LLVMBuildRetVoid(builder);
        }

        return null;
    }

    private int toDecimal(String number) {
        int result;

        if (number.startsWith("0x") || number.startsWith("0X")) {
            result = Integer.parseInt(number.substring(2), 16);
        } else if (number.length() > 1 && number.startsWith("0")) {
            result = Integer.parseInt(number.substring(1), 8);
        } else {
            result = Integer.parseInt(number);
        }

        return result;
    }

    private LLVMValueRef binaryOperation(String op, LLVMValueRef lhs, LLVMValueRef rhs) {

        switch(op) {
            case "+":
                return LLVMBuildAdd(builder, lhs, rhs, "tmp_");
            case "-":
                return LLVMBuildSub(builder, lhs, rhs, "tmp_");
            case "*":
                return LLVMBuildMul(builder, lhs, rhs, "tmp_");
            case "/":
                return LLVMBuildSDiv(builder, lhs, rhs, "tmp_");
            case "%":
                return LLVMBuildSRem(builder, lhs, rhs, "tmp_");
            default:
                break;
        }

        return null;
    }

    @Override
    public LLVMValueRef visitBlock(SysYParser.BlockContext ctx) {

        localCounter ++;

        LocalScope localScope = new LocalScope("local" + localCounter, currentScope);

        currentScope = localScope;

        visitChildren(ctx);

        currentScope = currentScope.getEnclosingScope();

        return null;
    }

    @Override
    public LLVMValueRef visitFuncCallExp(SysYParser.FuncCallExpContext ctx) {

        String functionName = ctx.IDENT().getText();

        LLVMValueRef function = globalScope.resolve(functionName);

        int argCount;

        if (ctx.funcRParams() == null) {
            argCount = 0;
        } else {
            argCount = ctx.funcRParams().param().size();
        }

        PointerPointer<Pointer> args = new PointerPointer<>(argCount);

        for (int i = 0; i < argCount; i ++) {
            args.put(i, visit(ctx.funcRParams().param(i)));
        }

        return LLVMBuildCall(builder, function, args, argCount, "");
    }

    @Override
    public LLVMValueRef visitLValExp(SysYParser.LValExpContext ctx) {

        LLVMValueRef lVal = visitLVal(ctx.lVal());


        return LLVMBuildLoad(builder, lVal, ctx.lVal().getText());
    }

    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();

        LLVMValueRef var =  currentScope.resolve(varName);

        if (ctx.L_BRACKT() == null || ctx.L_BRACKT().isEmpty()) {
            return var;
        }

        LLVMValueRef indexValue = visit(ctx.exp(0));

        LLVMValueRef[] arrayPointer = new LLVMValueRef[2];

        arrayPointer[0] = zero;

        arrayPointer[1] = indexValue;

        PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayPointer);

        LLVMValueRef elementPtr = LLVMBuildGEP(builder, var, indexPointer, 2, "GEP_tmp");

        return elementPtr;
    }

    private void initArrayElements(int elementCount, LLVMValueRef varPointer, LLVMValueRef[] initArray) {

        LLVMValueRef[] arrayPointer = new LLVMValueRef[2];

        arrayPointer[0] = zero;

        for (int i = 0; i < elementCount; i++) {
            arrayPointer[1] = LLVMConstInt(i32Type, i, 0);
            PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayPointer);
            LLVMValueRef elementPtr = LLVMBuildGEP(builder, varPointer, indexPointer, 2, "GEP_" + i);
            LLVMBuildStore(builder, initArray[i], elementPtr);
        }
    }

    @Override
    public LLVMValueRef visitVarDecl(SysYParser.VarDeclContext ctx) {
        for (SysYParser.VarDefContext varDefContext : ctx.varDef()) {
            String varName = varDefContext.IDENT().getText();
            LLVMTypeRef varType;

            if (varDefContext.L_BRACKT() == null || varDefContext.L_BRACKT().isEmpty()) {
                varType = i32Type;
            } else {
                int arraySize = (int) LLVMConstIntGetZExtValue(visit(varDefContext.constExp(0)));
                varType = LLVMArrayType(i32Type, arraySize);
            }

            LLVMValueRef varPointer = LLVMBuildAlloca(builder, varType, "pointer_" + varName);

            if (varDefContext.ASSIGN() != null) {
                SysYParser.ExpContext basicExpr = varDefContext.initVal().exp();
                if (basicExpr != null) {
                    LLVMValueRef initVal = visit(basicExpr);
                    LLVMBuildStore(builder, initVal, varPointer);
                } else {
                    int initCount = varDefContext.initVal().initVal().size();
                    int elementCount = (int) LLVMConstIntGetSExtValue(visit(varDefContext.constExp(0)));
                    LLVMValueRef[] initArray = new LLVMValueRef[elementCount];
                    for (int i = 0; i < initCount; i ++) {
                        initArray[i] = visit(varDefContext.initVal().initVal(i));
                    }

                    for (int i = initCount; i < elementCount; i ++) {
                        initArray[i] = zero;
                    }

                    initArrayElements(elementCount, varPointer, initArray);
                }
            }

            currentScope.define(varName, varPointer);
        }

        return null;
    }

    @Override
    public LLVMValueRef visitAssignStmt(SysYParser.AssignStmtContext ctx) {

        LLVMValueRef lValue = visit(ctx.lVal());

        LLVMValueRef rValue = visit(ctx.exp());

        LLVMBuildStore(builder, rValue, lValue);

        return null;
    }

    @Override
    public LLVMValueRef visitConstDecl(SysYParser.ConstDeclContext ctx) {

        for (SysYParser.ConstDefContext constDefContext : ctx.constDef()) {

            String varName = constDefContext.IDENT().getText();

            LLVMTypeRef varType;

            if (constDefContext.L_BRACKT() == null || constDefContext.L_BRACKT().isEmpty()) {
                varType = i32Type;
            } else {
                int arraySize = (int) LLVMConstIntGetZExtValue(visit(constDefContext.constExp(0)));
                varType = LLVMArrayType(i32Type, arraySize);
            }

            LLVMValueRef varPointer = LLVMBuildAlloca(builder, varType, "pointer_" + varName);

            SysYParser.ConstExpContext constExp = constDefContext.constInitVal().constExp();

            if (constExp != null) {
                LLVMValueRef constInitVal = visit(constExp);
                LLVMBuildStore(builder, constInitVal, varPointer);
            } else {

                int initCount = constDefContext.constInitVal().constInitVal().size();
                int elementCount = (int) LLVMConstIntGetSExtValue(visit(constDefContext.constExp(0)));
                LLVMValueRef[] initArray = new LLVMValueRef[elementCount];
                for (int i = 0; i < initCount; i ++) {
                    initArray[i] = visit(constDefContext.constInitVal().constInitVal(i));
                }

                for (int i = initCount; i < elementCount; i ++) {
                    initArray[i] = zero;
                }

                initArrayElements(elementCount, varPointer, initArray);
            }

            currentScope.define(varName, varPointer);
        }

        return null;
    }

    private LLVMTypeRef getTypeByName(String typeName) {
        if (typeName.equals("void")) {
            return voidType;
        }

        if (typeName.equals("int")) {
            return i32Type;
        }

        return null;
    }
}
