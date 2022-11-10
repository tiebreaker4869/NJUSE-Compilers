import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class SysYErrorListener extends BaseErrorListener {
    public boolean hasSyntaxError = false;
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        System.err.printf("Error type A at Line %d: syntax error, maybe unknown tokens.\n", line);
        hasSyntaxError = true;
    }
}
