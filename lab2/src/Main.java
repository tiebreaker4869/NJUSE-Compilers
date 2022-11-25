import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public class Main
{    
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);
        sysYParser.removeErrorListeners();
        SysYParseErrorListener listener = new SysYParseErrorListener();
        sysYParser.addErrorListener(listener);

        ParseTree tree = sysYParser.program();

        int errorCount = sysYParser.getNumberOfSyntaxErrors();

        if(errorCount == 0) {
            SysYMyVisitor visitor = new SysYMyVisitor();
            visitor.visit(tree);
        }
    }
}