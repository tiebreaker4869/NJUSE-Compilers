lexer grammar SysYLexer;

CONST : 'const';

INT : 'int';

VOID : 'void';

FLOAT: 'float';

IF : 'if';

ELSE : 'else';

WHILE : 'while';

BREAK : 'break';

CONTINUE : 'continue';

RETURN : 'return';

PLUS : '+';

MINUS : '-';

MUL : '*';

DIV : '/';

MOD : '%';

ASSIGN : '=';

EQ : '==';

NEQ : '!=';

LT : '<';

GT : '>';

LE : '<=';

GE : '>=';

NOT : '!';

AND : '&&';

OR : '||';

L_PAREN : '(';

R_PAREN : ')';

L_BRACE : '{';

R_BRACE : '}';

L_BRACKT : '[';

R_BRACKT : ']';

COMMA : ',';

SEMICOLON : ';';

//以下划线或字母开头，仅包含下划线、英文字母大小写、阿拉伯数字
IDENT: ('_' | ALPHA) ('_' | ALPHA | DECIMAL_DIGIT)*;

// 数字常量，包含十进制数，0开头的八进制数，0x或0X开头的十六进制数
INTEGR_CONST: '0' | [1-9][0-9]* | '0'OCT_DIGIT+ | ('0x' | '0X') HEX_DIGIT+;

WS: [ \r\n\t]+ -> skip;

LINE_COMMENT: '//' .*? '\n' -> skip;

MULTILINE_COMMENT: '/*' .*? '*/' -> skip;

fragment DECIMAL_DIGIT: [0-9];

fragment ALPHA: [a-zA-Z];

fragment OCT_DIGIT: [0-7];

fragment HEX_DIGIT: [0-9a-fA-F];