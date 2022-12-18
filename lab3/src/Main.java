import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import symbol.Symbol;
import type.Error;

import java.io.IOException;
import java.util.List;

public class Main
{

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("incorrect amount of arguments");
        }
        String source = args[0];
        int lineNumber = Integer.parseInt(args[1]);
        int columnNumber = Integer.parseInt(args[2]);
        String varName = args[3];

        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);
        sysYParser.removeErrorListeners();
        SysYParseErrorListener listener = new SysYParseErrorListener();
        sysYParser.addErrorListener(listener);

        ParseTree tree = sysYParser.program();
        SymTableVisitor visitor = new SymTableVisitor();
        tree.accept(visitor);

        if (Error.errorCount == 0) {
            SysYAnalyzeTreeVisitor analyzeTreeVisitor = new SysYAnalyzeTreeVisitor(lineNumber, columnNumber, varName);
            tree.accept(analyzeTreeVisitor);
        }
    }
}