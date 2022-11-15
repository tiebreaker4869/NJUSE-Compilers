public class DragonTokenizer extends Tokenizer {

    private Token WS(){
        while(Character.isWhitespace(this.peek)){
            this.advance();
        }

        return Token.WS;
    }

    private Token ID(){
        StringBuilder sb = new StringBuilder();

        do {
            sb.append(this.peek);
            this.advance();
        }while(Character.isLetterOrDigit(this.peek));

        Token token = this.keywordTable.getKeyWord(sb.toString());

        if(token == null){
            return new Token(sb.toString(), Token.Type.ID);
        }

        return token;
    }

    private Token NUMBER(){
        StringBuilder intStr = new StringBuilder();
        StringBuilder realStr = new StringBuilder();
        StringBuilder sciStr = new StringBuilder();

        int intPos = -1;
        int realPos = -1;

        int state = 13;

        while(true){
            switch(state){
                case 13:
                    if(Character.isDigit(this.peek)){
                        intStr.append(this.peek);
                        this.advance();
                    }else if(this.peek == '.'){
                        intPos = this.pos;
                        realStr.append(this.peek);
                        this.advance();
                        state = 14;
                    }else if(this.peek == 'e' || this.peek == 'E'){
                        intPos = this.pos;
                        sciStr.append(this.peek);
                        this.advance();
                        state = 16;
                    }else
                        return new Token(intStr.toString(), Token.Type.INT);
                    break;
                case 14:
                    if(Character.isDigit(this.peek)){
                        realStr.append(this.peek);
                        state = 15;
                        this.advance();
                    }else {
                        this.reset(intPos);
                        return new Token(intStr.toString(), Token.Type.INT);
                    }
                    break;
                case 15:
                    if(Character.isDigit(this.peek)){
                        realStr.append(this.peek);
                        this.advance();
                    }else if(this.peek == 'e' || this.peek == 'E'){
                        realPos = this.pos;
                        sciStr.append(this.peek);
                        this.advance();
                        state = 16;
                    }else
                        return new Token(intStr.append(realStr).toString(), Token.Type.REAL);
                    break;
                case 16:
                    if(this.peek == '+' || this.peek == '-'){
                        sciStr.append(this.peek);
                        this.advance();
                        state = 17;
                    }else if(Character.isDigit(this.peek)){
                        sciStr.append(this.peek);
                        this.advance();
                        state = 18;
                    }else {
                        if(realPos != -1){
                            this.reset(realPos);
                            return new Token(intStr.append(realStr).toString(), Token.Type.REAL);
                        }else {
                            this.reset(intPos);
                            return new Token(intStr.toString(), Token.Type.INT);
                        }
                    }
                    break;
                case 17:
                    if(Character.isDigit(this.peek)){
                        sciStr.append(this.peek);
                        this.advance();
                        state = 18;
                    }else {
                        if(realPos != -1) {
                            this.reset(realPos);
                            return new Token(intStr.append(realStr).toString(), Token.Type.REAL);
                        }else {
                            this.reset(intPos);
                            return new Token(intStr.toString(), Token.Type.INT);
                        }
                    }
                    break;
                case 18:
                    if(Character.isDigit(this.peek)){
                        sciStr.append(this.peek);
                        this.advance();
                    }else
                        return new Token(intStr.append(realStr).append(sciStr).toString(), Token.Type.SCI);
                    break;
                default:
                    break;
            }
        }
    }


    @Override
    public Token nextToken() {

        if(Character.isWhitespace(this.peek)){
            return WS();
        }

        if(Character.isLetter(this.peek)){
            return ID();
        }

        if(Character.isDigit(this.peek)){
            return NUMBER();
        }

        if(this.peek == '='){
            advance();
            return Token.EQ;
        }

        if(this.peek == '<'){
            advance();
            if(this.peek == '='){
                advance();
                return Token.LE;
            }else if(this.peek == '>'){
                advance();
                return Token.NEQ;
            }

            return Token.LT;
        }

        if(this.peek == '>'){
            advance();
            if(this.peek == '='){
                advance();
                return Token.GE;
            }

            return Token.GT;
        }

        if(this.peek == '+'){
            this.advance();
            return Token.ADD;
        }

        if(this.peek == '-'){
            this.advance();
            return Token.SUB;
        }

        if(this.peek == EOF){
            return Token.EOF;
        }
        // 不属于上面所有情况, 是未知字符
        return new Token(String.valueOf(this.peek), Token.Type.UNK);
    }
}
