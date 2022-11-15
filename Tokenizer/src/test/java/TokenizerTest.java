import org.junit.Test;

public class TokenizerTest {

    @Test
    public void nextTokenTest(){
        Tokenizer tokenizer = new DragonTokenizer();

        String filePath = "src/test/java/test.txt";

        tokenizer.readProgram(filePath);

        Token token;

        while((token = tokenizer.nextToken()) != Token.EOF){
            System.out.println(token);
        }
    }
}
