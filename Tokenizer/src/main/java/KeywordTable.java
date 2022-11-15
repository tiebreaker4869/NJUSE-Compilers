import java.util.HashMap;
import java.util.Map;

/**
 * 提前把保留关键字记录下来,和普通标识符区分
 */
public class KeywordTable {
    private Map<String, Token> keywordMap = new HashMap<>();

    public void reserve(Token keyword){
        keywordMap.put(keyword.text, keyword);
    }

    public Token getKeyWord(String keywordText){
        return keywordMap.get(keywordText);
    }
}
