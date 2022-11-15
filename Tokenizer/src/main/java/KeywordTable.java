import java.util.HashMap;
import java.util.Map;

public class KeywordTable {
    private Map<String, Token> keywordMap = new HashMap<>();

    public void reserve(Token keyword){
        keywordMap.put(keyword.text, keyword);
    }

    public Token getKeyWord(String keywordText){
        return keywordMap.get(keywordText);
    }
}
