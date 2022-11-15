import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public abstract class  Tokenizer {

    protected int pos;

    protected char peek;

    protected char[] programText;

    protected int maxPos;

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


    /**
     * 读取 filepath 所指定的程序文本
     * @param filePath 文件路径
     */
    public void readProgram(String filePath){
        // 读取程序并以字符数组的形式保存
        try(BufferedReader reader = new BufferedReader(new FileReader(filePath))){
            int c;
            StringBuilder sb = new StringBuilder();
            while((c = reader.read()) != -1){
                char ch = (char) c;
                sb.append(ch);
            }
            this.programText = sb.toString().toCharArray();
            this.pos = 0;
            this.peek = programText[0];
            this.maxPos = this.programText.length;
            KeywordTable.reserve(IF);
            KeywordTable.reserve(ELSE);
        }catch (IOException exception){
            System.out.println("IOException occurs when reading program!");
            exception.printStackTrace();
        }
    }

    protected void advance(){
        this.pos ++;
        if(this.pos >= maxPos){
            this.peek = (char) -1;
        }else{
            this.peek = programText[this.pos];
        }
    }

    protected void reset(int pos){
        this.pos = pos;
        this.peek = programText[pos];
    }

    public abstract Token nextToken();
}
