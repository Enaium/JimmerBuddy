/*
Copyright 2023 babyfish-ct

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
grammar Dto;

@header {
package cn.enaium.jimmer.buddy.dto;
}

// Parser --------

dto
    :
    exportStatement?
    importStatement*
    dtoType*
    EOF
    ;

exportStatement
    :
    EXPORT typeParts
    (RIGHT_ARROW PACKAGE packageParts)?
    ;

typeParts
    :
    qualifiedNameParts
    ;

packageParts
    :
    qualifiedNameParts
    ;

part
    : Identifier
    ;

importStatement
    :
    IMPORT qualifiedNameParts
    (
        DOT LEFT_BRACE importedType (COMMA importedType)* RIGHT_BRACE |
        AS alias
    )?
    ;

importedType
    :
    name (AS alias)?
    ;

alias
    : Identifier
    ;

dtoType
    :
    DocComment?
    annotation*
    (modifier)*
    name
    implements?
    dtoBody
    ;

implements
    : IMPLEMENTS typeRef (COMMA typeRef)*
    ;

modifier
    : INPUT | SPECIFICATION | UNSAFE | FIXED | STATIC | DYNAMIC | FUZZY
    ;

name
    : Identifier
    ;

dtoBody
    :
    LEFT_BRACE
    macro*
    ((explicitProp) (COMMA | SEMICOLON)?)*
    RIGHT_BRACE
    ;

explicitProp
    :
    aliasGroup | positiveProp | negativeProp | userProp
    ;

macro
    :
    HASH name
    (LEFT_PARENTHESIS qualifiedName (COMMA qualifiedName)* RIGHT_PARENTHESIS)?
    (QUESTION_MARK | EXCLAMATION_MARK)?
    ;

aliasGroup
    :
    aliasPattern aliasGroupBody
    ;

aliasGroupBody
    : LEFT_BRACE (macro)* (positiveProp)* RIGHT_BRACE
    ;

aliasPattern
    :
    AS LEFT_PARENTHESIS
    (POWER)?
    (original)?
    (DOLLAR)?
    (RIGHT_ARROW)
    (replacement)?
    RIGHT_PARENTHESIS
    ;

original
    : Identifier
    ;

replacement
    : Identifier
    ;

positiveProp
    :
    (DocComment)?
    (configuration | annotation)*
    PLUS?
    (modifier)?
    (
        (Identifier | NULL)
        (DIVIDE (Identifier)? (POWER)? (DOLLAR)?)?
        LEFT_PARENTHESIS prop (COMMA prop)* COMMA? RIGHT_PARENTHESIS
        |
        prop
    )
    (QUESTION_MARK | EXCLAMATION_MARK | MULTIPLY)?
    (AS alias)?
    (
        (DocComment)?
        (annotation)*
        implements?
        dtoBody
        |
        RIGHT_ARROW enumBody
    )?
    ;

negativeProp
    :
    MINUS prop
    ;

userProp
    :
    (DocComment)?
    (annotation)*
    prop COLON typeRef
    (
        EQUAL
        (MINUS)?
        (BooleanLiteral | IntegerLiteral | StringLiteral | FloatingPointLiteral | NULL)
    )?
    ;

prop
    : Identifier
    ;

typeRef
    :
    qualifiedName
    (LESS_THAN genericArgument (COMMA genericArgument)? GREATER_THAN)?
    (QUESTION_MARK)?
    ;

genericArgument
    :
    MULTIPLY |
    (modifier)? typeRef
    ;

qualifiedName
    :
    qualifiedNameParts
    ;


qualifiedNameParts
    : part (DOT part)*
    ;

configuration
    :
    where
    |
    orderBy
    |
    filter
    |
    recursion
    |
    fetchType
    |
    limit
    |
    batch
    |
    recursionDepth
    ;

where
    :
    CONFIG_WHERE LEFT_PARENTHESIS predicate RIGHT_PARENTHESIS
    ;

predicate
    :
    andPredicate (OR andPredicate)*
    ;

andPredicate
    :
    atomPredicate (AND atomPredicate)*
    ;

atomPredicate
    :
    LEFT_PARENTHESIS predicate RIGHT_PARENTHESIS
    |
    cmpPredicate
    |
    nullityPredicate
    ;

cmpPredicate
    :
    propPath
    (
        EQUAL propValue
        |
        DIAMOND propValue
        |
        NOT_EQUAL propValue
        |
        LESS_THAN propValue
        |
        LESS_THAN_EQUAL propValue
        |
        GREATER_THAN propValue
        |
        GREATER_THAN_EQUAL propValue
        |
        Identifier propValue
    )
    ;

nullityPredicate
    :
    propPath IS (NOT)? NULL
    ;

propPath
    :
    Identifier (DOT Identifier)*
    ;

propValue
    :
    BooleanLiteral |
    CharacterLiteral |
    SqlStringLiteral |
    (MINUS)?  IntegerLiteral |
    (MINUS)?  FloatingPointLiteral |
    ;

orderBy
    :
    CONFIG_ORDER_BY LEFT_PARENTHESIS orderByItem (COMMA orderByItem)* RIGHT_PARENTHESIS
    ;

orderByItem
    :
    propPath (Identifier)?
    ;

filter
    :
    CONFIG_FILTER LEFT_PARENTHESIS qualifiedName RIGHT_PARENTHESIS
    ;

recursion
    :
    CONFIG_RECURSION LEFT_PARENTHESIS qualifiedName RIGHT_PARENTHESIS
    ;

fetchType
    :
    CONFIG_FETCH_TYPE LEFT_PARENTHESIS Identifier RIGHT_PARENTHESIS
    ;

limit
    :
    CONFIG_LIMIT LEFT_PARENTHESIS IntegerLiteral (COMMA IntegerLiteral)? RIGHT_PARENTHESIS
    ;

batch
    :
    CONFIG_BATCH LEFT_PARENTHESIS IntegerLiteral RIGHT_PARENTHESIS
    ;

recursionDepth
    :
    CONFIG_DEPTH LEFT_PARENTHESIS IntegerLiteral RIGHT_PARENTHESIS
    ;

annotation
    :
    AT qualifiedName (LEFT_PARENTHESIS annotationArguments? RIGHT_PARENTHESIS)?
    ;

annotationArguments
    :
    annotationValue (COMMA annotationNamedArgument)*
    |
    annotationNamedArgument (COMMA annotationNamedArgument)*
    ;

annotationNamedArgument
    :
    name EQUAL annotationValue
    ;

annotationValue
    :
    annotationSingleValue
    |
    annotationArrayValue
    ;

annotationSingleValue
    :
    BooleanLiteral |
    CharacterLiteral |
    StringLiteral (PLUS StringLiteral)* |
    (MINUS)? IntegerLiteral |
    (MINUS)? FloatingPointLiteral |
    qualifiedName classSuffix? |
    annotation |
    nestedAnnotation
    ;

annotationArrayValue
    :
    LEFT_BRACE annotationSingleValue (COMMA annotationSingleValue)* RIGHT_BRACE
    |
    LEFT_BRACKET annotationSingleValue (COMMA annotationSingleValue)* RIGHT_BRACKET
    ;

nestedAnnotation
    :
    qualifiedName LEFT_PARENTHESIS annotationArguments? RIGHT_PARENTHESIS
    ;

enumBody
    :
    LEFT_BRACE (enumMapping (COMMA|SEMICOLON)?)+ RIGHT_BRACE
    ;

enumMapping
    :
    name COLON
    (
        StringLiteral | (MINUS)? IntegerLiteral
    )
    ;

classSuffix
    :
    QUESTION_MARK? (DOT | DOUBLE_COLON) CLASS
    ;
// Lexer --------

BooleanLiteral
    :
    TRUE | FALSE
    ;

RIGHT_ARROW  : '->';
DOT : '.';
COMMA : ',';
SEMICOLON : ';';
LEFT_BRACE : '{';
RIGHT_BRACE : '}';
LEFT_BRACKET : '[';
RIGHT_BRACKET : ']';
HASH : '#';
LEFT_PARENTHESIS : '(';
RIGHT_PARENTHESIS : ')';
QUESTION_MARK : '?';
EXCLAMATION_MARK : '!';
POWER : '^';
DOLLAR : '$';
PLUS : '+';
DIVIDE : '/';
MULTIPLY : '*';
MINUS : '-';
COLON : ':';
EQUAL : '=';
LESS_THAN : '<';
GREATER_THAN : '>';
AT : '@';
DOUBLE_COLON : '::';
DIAMOND : '<>';
NOT_EQUAL : '!=';
LESS_THAN_EQUAL : '<=';
GREATER_THAN_EQUAL : '>=';

EXPORT : 'export';
PACKAGE : 'package';
IMPORT : 'import';
AS : 'as';
INPUT : 'input';
SPECIFICATION : 'specification';
UNSAFE : 'unsafe';
FIXED : 'fixed';
STATIC : 'static';
DYNAMIC : 'dynamic';
FUZZY : 'fuzzy';
IMPLEMENTS : 'implements';
NULL : 'null';
CONFIG_WHERE : '!where';
OR : 'or';
AND : 'and';
IS  : 'is';
NOT : 'not';
CONFIG_ORDER_BY : '!orderBy';
CONFIG_FILTER : '!filter';
CONFIG_RECURSION : '!recursion';
CONFIG_FETCH_TYPE : '!fetchType';
CONFIG_LIMIT : '!limit';
CONFIG_BATCH : '!batch';
CONFIG_DEPTH : '!depth';
CLASS : 'class';
TRUE : 'true';
FALSE : 'false';
SINGLE_QUOTE: '\'';
DOUBLE_QUOTE: '"';

Identifier
    :
    [$A-Za-z_][$A-Za-z_0-9]*
    ;

WhiteSpace
    :
    (' ' | '\u0009' | '\u000C' | '\r' | '\n') -> channel(HIDDEN)
    ;

DocComment
    :
    ('/**' .*? '*/')
    ;

BlockComment
    :
    ('/*' .*? '*/') -> channel(HIDDEN)
    ;

LineComment
    :
    ('//' ~[\r\n]* ('\r\n' | '\r' | '\n')?) -> channel(HIDDEN)
    ;

SqlStringLiteral
    :
    SINGLE_QUOTE ( ~'\'' | '\'\'' )* SINGLE_QUOTE
    ;

CharacterLiteral
    :
    SINGLE_QUOTE SingleCharacter SINGLE_QUOTE
    |
    SINGLE_QUOTE EscapeSequence SINGLE_QUOTE
    ;

fragment
SingleCharacter
    :
    ~['\\\r\n]
    ;

StringLiteral
    :
    DOUBLE_QUOTE StringCharacters? DOUBLE_QUOTE
    ;

fragment
StringCharacters
    :
    StringCharacter+
    ;

fragment
StringCharacter
    :
    ~["\\\r\n] | EscapeSequence
    ;

fragment
EscapeSequence
    :
    '\\' [btnfr"'\\]
    |
    UnicodeEscape // This is not in the spec but prevents having to preprocess the input
    ;

fragment
UnicodeEscape
    :
    '\\' 'u'+  HexDigit HexDigit HexDigit HexDigit
    ;

fragment
HexDigit
    :
    [0-9] | [a-f] | [A-F]
    ;

IntegerLiteral
    :
    '0' | [1-9][0-9]*
    ;

FloatingPointLiteral
    :
    [0-9]+ DOT [0-9]+
    ;

ERRCHAR
    :   .   -> channel(HIDDEN)
    ;
