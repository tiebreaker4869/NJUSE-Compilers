import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import symbol.GlobalScope;
import symbol.LocalScope;
import symbol.Scope;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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

    private LLVMValueRef currentFunction;

    private LinkedList<LLVMBasicBlockRef> breakStack = new LinkedList<>();

    private LinkedList<LLVMBasicBlockRef> continueStack = new LinkedList<>();

    private Map<String, String> returnTypes = new HashMap<>();

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

        returnTypes.put(functionName, ctx.funcType().getText());

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

        currentFunction = function;

        visitBlock(ctx.block());

        currentFunction = null;

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

        if (returnTypes.get(functionName).equals("void")) {
            return LLVMBuildCall(builder, function, args, argCount, "");
        }

        return LLVMBuildCall(builder, function, args, argCount, "tmp_");
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
            LLVMValueRef varPointer;

            if (currentScope != globalScope) {
                varPointer = LLVMBuildAlloca(builder, varType, "pointer_" + varName);
            } else {
                varPointer = LLVMAddGlobal(module, varType, "global_" + varName);
            }

            if (varDefContext.ASSIGN() != null) {
                SysYParser.ExpContext basicExpr = varDefContext.initVal().exp();
                if (basicExpr != null) {
                    LLVMValueRef initVal = visit(basicExpr);
                    if (currentScope != globalScope) {
                        LLVMBuildStore(builder, initVal, varPointer);
                    } else {
                        LLVMSetInitializer(varPointer, initVal);
                    }
                } else {
                    if (currentScope != globalScope) {
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
                    } else {
                        //处理全局数组显式初始化
                        int initCount = varDefContext.initVal().initVal().size();
                        int elementCount = (int) LLVMConstIntGetSExtValue(visit(varDefContext.constExp(0)));
                        PointerPointer<Pointer> initVals = new PointerPointer<>(elementCount);
                        for (int i = 0; i < elementCount; i ++) {
                            if (i < initCount) {
                                initVals.put(i, visit(varDefContext.initVal().initVal(i)));
                            } else {
                                initVals.put(i, zero);
                            }
                        }

                        LLVMValueRef initValsArray = LLVMConstArray(i32Type, initVals, elementCount);
                        LLVMSetInitializer(varPointer, initValsArray);
                    }
                }
            } else {
                if (varDefContext.L_BRACKT() == null || varDefContext.L_BRACKT().isEmpty()) {
                    LLVMSetInitializer(varPointer, zero);
                } else {
                    //处理全局数组默认初始化
                    int elementCount = (int) LLVMConstIntGetSExtValue(visit(varDefContext.constExp(0)));
                    PointerPointer<Pointer> constantVals = new PointerPointer<>(elementCount);

                    for (int i = 0; i < elementCount; i ++) {
                        constantVals.put(i, zero);
                    }

                    LLVMValueRef constValsArray = LLVMConstArray(i32Type, constantVals, elementCount);

                    LLVMSetInitializer(varPointer, constValsArray);
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

            LLVMValueRef varPointer;

            if (currentScope != globalScope) {
                varPointer = LLVMBuildAlloca(builder, varType, "pointer_" + varName);
            } else {
                varPointer = LLVMAddGlobal(module, varType, "global_" + varName);
            }


            SysYParser.ConstExpContext constExp = constDefContext.constInitVal().constExp();

            if (constExp != null) {
                LLVMValueRef constInitVal = visit(constExp);
                if (currentScope != globalScope) {
                    LLVMBuildStore(builder, constInitVal, varPointer);
                } else {
                    LLVMSetInitializer(varPointer, constInitVal);
                }
            } else {
                int initCount = constDefContext.constInitVal().constInitVal().size();
                int elementCount = (int) LLVMConstIntGetSExtValue(visit(constDefContext.constExp(0)));

                if (currentScope != globalScope) {
                    LLVMValueRef[] initArray = new LLVMValueRef[elementCount];
                    for (int i = 0; i < initCount; i ++) {
                        initArray[i] = visit(constDefContext.constInitVal().constInitVal(i));
                    }

                    for (int i = initCount; i < elementCount; i ++) {
                        initArray[i] = zero;
                    }

                    initArrayElements(elementCount, varPointer, initArray);
                } else {
                    PointerPointer<Pointer> constVals = new PointerPointer<>(elementCount);
                    for (int i = 0; i < elementCount; i ++) {
                        if (i < initCount) {
                            constVals.put(i, visit(constDefContext.constInitVal().constInitVal(i)));
                        } else {
                            constVals.put(i, zero);
                        }
                    }

                    LLVMValueRef constValsArray = LLVMConstArray(i32Type, constVals, elementCount);

                    LLVMSetInitializer(varPointer, constValsArray);
                }
            }

            currentScope.define(varName, varPointer);
        }

        return null;
    }

    @Override
    public LLVMValueRef visitIfStmt(SysYParser.IfStmtContext ctx) {
        LLVMBasicBlockRef cond = LLVMAppendBasicBlock(currentFunction, "condition");
        LLVMBasicBlockRef trueBranch = LLVMAppendBasicBlock(currentFunction, "if_true");
        LLVMBasicBlockRef falseBranch = null;

        if (ctx.ELSE() != null) {
            falseBranch = LLVMAppendBasicBlock(currentFunction, "if_false");
        }

        LLVMBasicBlockRef exit = LLVMAppendBasicBlock(currentFunction, "exit");

        // 无条件跳转到 cond 计算布尔值
        LLVMBuildBr(builder, cond);
        LLVMPositionBuilderAtEnd(builder, cond);
        LLVMValueRef condValue = visit(ctx.cond());
        condValue = LLVMBuildZExt(builder, condValue, i32Type, "tmp_");
        condValue = LLVMBuildICmp(builder, LLVMIntNE, condValue, zero, "tmp_");

        if (falseBranch != null) {
            LLVMBuildCondBr(builder, condValue, trueBranch, falseBranch);
        } else {
            LLVMBuildCondBr(builder, condValue, trueBranch, exit);
        }


        LLVMPositionBuilderAtEnd(builder, trueBranch);
        visit(ctx.stmt(0));
        LLVMBuildBr(builder, exit);

        if (ctx.ELSE() != null) {
            LLVMPositionBuilderAtEnd(builder, falseBranch);
            int stmtCount = ctx.stmt().size();

            for (int i = 1; i < stmtCount; i ++) {
                visit(ctx.stmt(i));
            }

            LLVMBuildBr(builder, exit);
        }

        LLVMPositionBuilderAtEnd(builder, exit);

        return null;
    }

    @Override
    public LLVMValueRef visitWhileStmt(SysYParser.WhileStmtContext ctx) {
        LLVMBasicBlockRef cond = LLVMAppendBasicBlock(currentFunction, "condition");
        LLVMBasicBlockRef loopBody = LLVMAppendBasicBlock(currentFunction, "loop");
        LLVMBasicBlockRef exit = LLVMAppendBasicBlock(currentFunction, "exit");

        LLVMBuildBr(builder, cond);

        LLVMPositionBuilderAtEnd(builder, cond);
        LLVMValueRef condVal = visit(ctx.cond());
        condVal = LLVMBuildZExt(builder, condVal, i32Type, "tmp_");
        condVal = LLVMBuildICmp(builder, LLVMIntNE, condVal, zero, "tmp_");

        LLVMBuildCondBr(builder, condVal, loopBody, exit);

        LLVMPositionBuilderAtEnd(builder, loopBody);

        breakStack.addFirst(exit);
        continueStack.addFirst(cond);
        visit(ctx.stmt());
        continueStack.pop();
        breakStack.pop();


        LLVMBuildBr(builder, cond);
        LLVMPositionBuilderAtEnd(builder, exit);

        return null;
    }

    @Override
    public LLVMValueRef visitBreakStmt(SysYParser.BreakStmtContext ctx) {

        LLVMBasicBlockRef exit = breakStack.peek();

        LLVMBuildBr(builder, exit);

        return null;
    }

    @Override
    public LLVMValueRef visitContinueStmt(SysYParser.ContinueStmtContext ctx) {

        LLVMBasicBlockRef cond = continueStack.peek();

        LLVMBuildBr(builder, cond);

        return null;
    }

    @Override
    public LLVMValueRef visitCompareExp(SysYParser.CompareExpContext ctx) {

        if (ctx.GT() != null) {
            return LLVMBuildICmp(builder, LLVMIntSGT, LLVMBuildZExt(builder, visit(ctx.cond(0)), i32Type, "tmp_"), LLVMBuildZExt(builder, visit(ctx.cond(1)), i32Type, "tmp_"), "tmp_");
        }

        if (ctx.GE() != null) {
            return LLVMBuildICmp(builder, LLVMIntSGE,  LLVMBuildZExt(builder, visit(ctx.cond(0)), i32Type, "tmp_"), LLVMBuildZExt(builder, visit(ctx.cond(1)), i32Type, "tmp_"), "tmp_");
        }

        if (ctx.LT() != null) {
            return LLVMBuildICmp(builder, LLVMIntSLT,  LLVMBuildZExt(builder, visit(ctx.cond(0)), i32Type, "tmp_"), LLVMBuildZExt(builder, visit(ctx.cond(1)), i32Type, "tmp_"), "tmp_");
        }

        if (ctx.LE() != null) {
            return LLVMBuildICmp(builder, LLVMIntSLE,  LLVMBuildZExt(builder, visit(ctx.cond(0)), i32Type, "tmp_"), LLVMBuildZExt(builder, visit(ctx.cond(1)), i32Type, "tmp_"), "tmp_");
        }

        return null;
    }

    @Override
    public LLVMValueRef visitRelationExp(SysYParser.RelationExpContext ctx) {
        if (ctx.EQ() != null) {
            return LLVMBuildICmp(builder, LLVMIntEQ,  visit(ctx.cond(0)), visit(ctx.cond(1)), "tmp_");
        }

        return LLVMBuildICmp(builder, LLVMIntNE, visit(ctx.cond(0)), visit(ctx.cond(1)), "tmp_");
    }

    @Override
    public LLVMValueRef visitCondExp(SysYParser.CondExpContext ctx) {
        LLVMValueRef cond = visit(ctx.exp());

        return cond;
    }

    @Override
    public LLVMValueRef visitAndExp(SysYParser.AndExpContext ctx) {
        return LLVMBuildAnd(builder, LLVMBuildZExt(builder, visit(ctx.cond(0)), i32Type, "tmp_"), LLVMBuildZExt(builder, visit(ctx.cond(1)), i32Type, "tmp_"), "tmp_");
    }

    @Override
    public LLVMValueRef visitOrExp(SysYParser.OrExpContext ctx) {
        return LLVMBuildOr(builder,  LLVMBuildZExt(builder, visit(ctx.cond(0)), i32Type, "tmp_"), LLVMBuildZExt(builder, visit(ctx.cond(1)), i32Type, "tmp_"), "tmp_");
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
