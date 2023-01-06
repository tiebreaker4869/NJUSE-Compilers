import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;


public class Main
{

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("input path is required");
        }
        String source = args[0];
        String target = args[1];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);
        ParseTree parseTree = sysYParser.program();
        LLVMIRVisitor visitor = new LLVMIRVisitor(target);
        visitor.visit(parseTree);
    }
}