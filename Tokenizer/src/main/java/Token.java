/**
 * Token class, represent tokens in the program
 */
public class Token {
    public enum Type {
        WS, INT, REAL, SCI, ID, IF, ELSE, LT, LE, GT, GE, NEQ, EQ, UNK, EOF, ADD, SUB
    }

    public static final Token IF = new Token("if", Token.Type.IF);
    public static final Token ELSE = new Token("else", Token.Type.ELSE);
    public static final Token LT = new Token("<", Token.Type.LT);
    public static final Token LE = new Token("<=", Token.Type.LE);
    public static final Token GT = new Token(">", Token.Type.GT);
    public static final Token GE = new Token(">=", Token.Type.GE);
    public static final Token EQ = new Token("=", Token.Type.EQ);
    public static final Token NEQ = new Token("<>", Token.Type.NEQ);
    public static final Token EOF = new Token("EOF", Token.Type.EOF);
    public static final Token WS = new Token(" ", Token.Type.WS);
    public static final Token ADD = new Token("+", Token.Type.ADD);
    public static final Token SUB = new Token("-", Token.Type.SUB);

    public Token(String text, Type type){
        this.text = text;
        this.type = type;
    }

    public String text;

    public Type type;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Token{" +
                "text='" + text + '\'' +
                ", type=" + type +
                '}';
    }
}
