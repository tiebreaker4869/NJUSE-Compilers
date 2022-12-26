import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.llvm.LLVM.*;

import java.util.LinkedList;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMIRVisitor extends SysYParserBaseVisitor<LLVMValueRef> {

    private LLVMModuleRef module = LLVMModuleCreateWithName("module");

    private LLVMTypeRef i32Type = LLVMInt32Type();

    private LLVMBuilderRef builder = LLVMCreateBuilder();

    private LinkedList<LLVMValueRef> stack = new LinkedList<>();

    private String targetPath;

    public LLVMIRVisitor(String outputPath) {
        this.targetPath = outputPath;
        //初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();
    }

    @Override
    public LLVMValueRef visitReturnStmt(SysYParser.ReturnStmtContext ctx) {
        LLVMValueRef result = visit(ctx.exp());
        LLVMBasicBlockRef mainEntry = LLVMAppendBasicBlock(stack.peek(), "mainEntry");
        LLVMPositionBuilderAtEnd(builder, mainEntry);

        LLVMBuildRet(builder, result);

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
            return LLVMConstInt(i32Type, number, 1);
        }

        return null;
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        LLVMTypeRef returnType = i32Type;

        LLVMTypeRef functionType = LLVMFunctionType(returnType, LLVMVoidType(), 0, 0);

        LLVMValueRef main = LLVMAddFunction(module, "main", functionType);

        stack.addLast(main);

        visitBlock(ctx.block());

        stack.pop();

        BytePointer error = new BytePointer();
        LLVMPrintModuleToFile(module,targetPath,error);

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

        long left = LLVMConstIntGetZExtValue(lhs);

        long right = LLVMConstIntGetZExtValue(rhs);

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
}
