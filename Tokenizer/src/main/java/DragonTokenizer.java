public class DragonTokenizer extends Tokenizer {

    private Token WS(){
        while(Character.isWhitespace(this.peek)){
            this.advance();
        }

        return Tokenizer.WS;
    }

    private Token ID(){
        StringBuilder sb = new StringBuilder();

        do {
            sb.append(this.peek);
            this.advance();
        }while(Character.isLetterOrDigit(this.peek));

        Token token = KeywordTable.getKeyWord(sb.toString());

        if(token == null){
            return new Token(sb.toString(), Token.Type.ID);
        }

        return token;
    }


    @Override
    public Token nextToken() {

        return null;
    }
}
