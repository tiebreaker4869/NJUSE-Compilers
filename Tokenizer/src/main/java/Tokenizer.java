import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public abstract class  Tokenizer {

    protected char EOF = (char) -1;

    protected int pos;

    protected char peek;

    protected char[] programText;

    protected int maxPos;

    protected KeywordTable keywordTable;




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
            this.keywordTable = new KeywordTable();
            keywordTable.reserve(Token.IF);
            keywordTable.reserve(Token.ELSE);
        }catch (IOException exception){
            System.out.println("IOException occurs when reading program!");
            exception.printStackTrace();
        }
    }

    protected void advance(){
        this.pos ++;
        if(this.pos >= maxPos){
            this.peek = EOF;
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
