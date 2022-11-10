import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;

import java.io.IOException;
import java.util.List;

public class Main
{    
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);

        SysYErrorListener listener = new SysYErrorListener();
        sysYLexer.removeErrorListeners();
        sysYLexer.addErrorListener(listener);

        List<? extends Token> tokens = sysYLexer.getAllTokens();

        if(listener.hasSyntaxError){
            return;
        }

        Vocabulary vocab = sysYLexer.getVocabulary();
        for(Token token : tokens){
            String symbolicName = vocab.getSymbolicName(token.getType());
            String tokenText = token.getText();
            Integer lineNum = token.getLine();
            if(symbolicName.equals("INTEGR_CONST")){
                tokenText = toDecimal(tokenText);
            }
            System.err.printf("%s %s at Line %d.\n", symbolicName, tokenText, lineNum);
        }
    }

    private static String toDecimal(String number){
        Integer num;
        if(number.startsWith("0X") || number.startsWith("0x")){
            num = Integer.parseInt(number.substring(2), 16);
        }else if(number.startsWith("0") &&number.length() > 1){
            num = Integer.parseInt(number.substring(1), 8);
        }else {
            num = Integer.parseInt(number);
        }

        return num.toString();
    }
}