import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class SysYParseErrorListener extends BaseErrorListener {
    public boolean hasSyntaxError;
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        hasSyntaxError = true;
        System.err.printf("Error type B at Line %d:%s\n", line, msg);
    }
}
