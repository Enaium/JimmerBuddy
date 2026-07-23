package cn.enaium.jimmer.buddy.extensions.dto.lexer;

import cn.enaium.jimmer.buddy.extensions.dto.DtoParserDefinitionKt;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static cn.enaium.jimmer.buddy.extensions.dto.psi.DtoTypes.*;

%%

%class DtoLexer
%implements FlexLexer
%unicode
%public
%function advance
%type IElementType

LINE_TERMINATOR     = \r|\n|\r\n
INPUT_CHARACTER     = [^\r\n]
WHITE_SPACE         = [ \t\f\r\n]+

DOC_COMMENT         = "/**" [^*]* "*"+ ([^/*][^*]* "*"+)* "/"
BLOCK_COMMENT       = "/*" [^*]* "*"+ ([^/*][^*]* "*"+)* "/"
LINE_COMMENT        = "//" {INPUT_CHARACTER}* {LINE_TERMINATOR}?

UNICODE_ESCAPE      = \\u+ [0-9a-fA-F]{4}
ESCAPE_SEQUENCE     = \\[btnfr\"\\'\\] | {UNICODE_ESCAPE}

IDENTIFIER         = [$A-Za-z_][$A-Za-z_0-9]*

INTEGER_LITERAL     = 0 | [1-9][0-9]*
FLOATING_POINT_LITERAL = [0-9]+ "." [0-9]+

SQL_STRING_LITERAL   = \' ([^\'] | \'\')* \'
CHARACTER_LITERAL   = \' ( [^\'\\\r\n] | {ESCAPE_SEQUENCE} ) \'
STRING_LITERAL      = \" ( [^\"\\] | {ESCAPE_SEQUENCE} )* \"

%%

{DOC_COMMENT}        { return DOC_COMMENT; }
{BLOCK_COMMENT}      { return DtoParserDefinitionKt.getBLOCK_COMMENT(); }
{LINE_COMMENT}       { return DtoParserDefinitionKt.getLINE_COMMENT(); }

{WHITE_SPACE}        { return TokenType.WHITE_SPACE; }

"!where"            { return CONFIG_WHERE; }
"!orderBy"          { return CONFIG_ORDER_BY; }
"!filter"           { return CONFIG_FILTER; }
"!recursion"        { return CONFIG_RECURSION; }
"!fetchType"        { return CONFIG_FETCH_TYPE; }
"!limit"            { return CONFIG_LIMIT; }
"!batch"            { return CONFIG_BATCH; }
"!depth"            { return CONFIG_DEPTH; }

"->"                { return ARROW; }
"::"                { return COLON2; }
"<>"                { return NE1; }
"!="                { return NE2; }
"<="                { return LE; }
">="                { return GE; }

"<"                 { return LT; }
">"                 { return GT; }
"="                 { return EQ; }
"!"                 { return EXCLAM; }
"@"                 { return AT; }
"^"                 { return CARET; }
"$"                 { return DOLLAR; }
"/"                 { return SLASH; }
"."                 { return DOT; }
":"                 { return COLON; }
"?"                 { return QUES; }
"*"                 { return STAR; }
"+"                 { return PLUS; }
"-"                 { return MINUS; }
"{"                 { return LBRACE; }
"}"                 { return RBRACE; }
"("                 { return LPAREN; }
")"                 { return RPAREN; }
"["                 { return LBRACK; }
"]"                 { return RBRACK; }
","                 { return COMMA; }
";"                 { return SEMI; }
"#"                 { return HASH; }

"package"           { return PACKAGE; }
"export"            { return EXPORT; }
"import"            { return IMPORT; }
"fragment"          { return FRAGMENT; }
"for"               { return FOR; }
"as"                { return AS; }
"implements"        { return IMPLEMENTS; }
"input"             { return INPUT; }
"specification"     { return SPECIFICATION; }
"unsafe"            { return UNSAFE; }
"sealed"            { return SEALED; }
"fixed"             { return FIXED; }
"static"            { return STATIC; }
"dynamic"           { return DYNAMIC; }
"fuzzy"             { return FUZZY; }
"class"             { return CLASS; }
"default"           { return DEFAULT; }
"null"              { return NULL; }
"fold"              { return FOLD; }
"and"               { return AND; }
"or"                { return OR; }
"is"                { return IS; }
"not"               { return NOT; }

"true"              { return BOOLEAN_LITERAL; }
"false"             { return BOOLEAN_LITERAL; }

{SQL_STRING_LITERAL}  { return SQL_STRING_LITERAL; }
{CHARACTER_LITERAL}  { return CHARACTER_LITERAL; }
{STRING_LITERAL}     { return STRING_LITERAL; }
{INTEGER_LITERAL}    { return INTEGER_LITERAL; }
{FLOATING_POINT_LITERAL} { return FLOATING_POINT_LITERAL; }

{IDENTIFIER}        { return IDENTIFIER; }

[^]                 { return BAD_CHARACTER; }