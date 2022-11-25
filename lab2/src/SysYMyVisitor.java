import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.Map;


public class SysYMyVisitor extends SysYParserBaseVisitor<Void> {
    public static Map<String, String> map;
    static {
        map = new HashMap<>();
        String[] orangeGroup = {
                "CONST",
                "INT",
                "VOID",
                "IF",
                "ELSE",
                "WHILE",
                "BREAK",
                "CONTINUE",
                "RETURN",
        };
        String[] blueGroup = {
                "PLUS",
                "MINUS",
                "MUL",
                "DIV",
                "MOD",
                "ASSIGN",
                "EQ",
                "NEQ",
                "LT",
                "GT",
                "LE",
                "GE",
                "NOT",
                "AND",
                "OR",
        };
        String[] redGroup = {"IDENT"};

        String[] greenGroup = {"INTEGR_CONST"};

        for(String s : orangeGroup){
            map.put(s, "[orange]");
        }

        for(String s : blueGroup){
            map.put(s, "[blue]");
        }

        for(String s : greenGroup){
            map.put(s, "[green]");
        }

        for(String s : redGroup){
            map.put(s, "[red]");
        }
    }
    @Override
    public Void visitChildren(RuleNode node) {

        if(node.getChildCount() > 0){
            String prefix = getPrefix(node);
            int ruleIndex = node.getRuleContext().getRuleIndex();
            String ruleName = SysYParser.ruleNames[ruleIndex];
            System.err.println(prefix + capitalizeFirstLetter(ruleName));

            for(int i = 0; i < node.getChildCount(); i ++){
                ParseTree child = node.getChild(i);
                child.accept(this);
            }
        }

        return null;
    }

    @Override
    public Void visitTerminal(TerminalNode node) {

        String prefix = getPrefix(node);

        String tokenText = node.getSymbol().getText();
        Integer tokenType = node.getSymbol().getType();
        String tokenTypeName = SysYLexer.VOCABULARY.getSymbolicName(tokenType);
        String color = map.get(tokenTypeName);
        if(color != null){
            if(tokenTypeName.equals("INTEGR_CONST")){
                tokenText = toDecimal(tokenText);
            }
            System.err.println(prefix + tokenText + " " + tokenTypeName + color);
        }

        return null;
    }

    private int getCurrentHeight(ParseTree root) {
        int height = 0;
        while (root.getParent() != null) {
            height++;
            root = root.getParent();
        }

        return height;
    }

    private String getPrefix(ParseTree root) {
        int height = getCurrentHeight(root);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < height; i++) {
            sb.append("  ");
        }

        return sb.toString();
    }

    private String toDecimal(String number){
        if(number.startsWith("0x") || number.startsWith("0X")){
            return String.valueOf(Integer.parseInt(number.substring(2), 16));
        }else if(number.length() > 1 && number.startsWith("0")){
            return String.valueOf(Integer.parseInt(number.substring(1), 8));
        }

        return number;
    }

    private String capitalizeFirstLetter(String s){
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
