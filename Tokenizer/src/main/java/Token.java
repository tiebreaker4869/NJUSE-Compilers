/**
 * Token class, represent tokens in the program
 */
public class Token {
    public enum Type {
        WS, INT, REAL, SCI, ID, IF, ELSE, LT, LE, GT, GE, NEQ, EQ, UNK, EOF
    }

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
}
