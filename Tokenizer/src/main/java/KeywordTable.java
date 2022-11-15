import java.util.HashMap;
import java.util.Map;

public class KeywordTable {
    private static Map<String, Token> keywordMap = new HashMap<>();

    public static void reserve(Token keyword){
        keywordMap.put(keyword.text, keyword);
    }

    public static Token getKeyWord(String keywordText){
        return keywordMap.get(keywordText);
    }
}
