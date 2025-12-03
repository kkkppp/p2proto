grammar Formula;

// Entry
parse
    : expr EOF
    ;

// Expressions
expr
    : functionCall
    | atom
    ;

functionCall
    : IDENT LPAREN argList? RPAREN
    ;

argList
    : expr (COMMA expr)*
    ;

atom
    : VARIABLE
    | STRING
    | NUMBER
    ;

// Lexer
VARIABLE
    : '$' IDENT
    ;

STRING
    : '\'' ( '\\\'' | ~'\'' )* '\''
    ;

// Integers only for simplicity (substring positions/length)
NUMBER
    : [0-9]+
    ;

IDENT
    : [a-zA-Z_] [a-zA-Z_0-9]*
    ;

LPAREN  : '(' ;
RPAREN  : ')' ;
COMMA   : ',' ;

WS
    : [ \t\r\n]+ -> skip
    ;
