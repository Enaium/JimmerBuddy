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
    (importStatements += importStatement)*
    (dtoTypes+=dtoType)*
    EOF
    ;

exportStatement
    :
    EXPORT typeParts
    (RIGHT_ARROR PACKAGE packageParts)?
    ;

typeParts
    :
    part (DOT part)*
    ;

packageParts
    :
    part (DOT part)*
    ;

part
    : Identifier
    ;

importStatement
    :
    IMPORT qualifiedNameParts
    (
        DOT LEFT_BRACE importedTypes += importedType (COMMA importedTypes += importedType)* RIGHT_BRACE |
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
    (doc = DocComment)?
    (annotations += annotation)*
    (modifier)*
    name
    (IMPLEMENTS superInterfaces += typeRef (COMMA superInterfaces += typeRef)*)?
    body=dtoBody
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
    (macros += macro)*
    ((explicitProps += explicitProp) (COMMA | SEMICOLON)?)*
    RIGHT_BRACE
    ;

explicitProp
    :
    aliasGroup | positiveProp | negativeProp | userProp
    ;

macro
    :
    HASH name
    (LEFT_PARENTHESIS args+=qualifiedName (COMMA args+=qualifiedName)* RIGHT_PARENTHESIS)?
    (optional = QUESTION_MARK | required = EXCLAMATION_MARK)?
    ;

aliasGroup
    :
    pattern = aliasPattern aliasGroupBody
    ;

aliasGroupBody
    : LEFT_BRACE (macros += macro)* (props += positiveProp)* RIGHT_BRACE
    ;

aliasPattern
    :
    AS LEFT_PARENTHESIS
    (prefix = POWER)?
    (original = Identifier)?
    (suffix = DOLLAR)?
    (translator = RIGHT_ARROR)
    (replacement = Identifier)?
    RIGHT_PARENTHESIS
    ;

positiveProp
    :
    (doc = DocComment)?
    (configurations += configuration | annotations += annotation)*
    PLUS?
    (modifier)?
    (
        (func = Identifier | func = NULL)
        (flag = DIVIDE (insensitive = Identifier)? (prefix = POWER)? (suffix = DOLLAR)?)?
        LEFT_PARENTHESIS props += prop (COMMA props += prop)* COMMA? RIGHT_PARENTHESIS
        |
        props += prop
    )
    (optional = QUESTION_MARK | required = EXCLAMATION_MARK | recursive = MULTIPLY)?
    (AS alias)?
    (
        (childDoc = DocComment)?
        (bodyAnnotations += annotation)*
        (IMPLEMENTS bodySuperInterfaces += typeRef (COMMA bodySuperInterfaces += typeRef)*)?
        dtoBody
        |
        RIGHT_ARROR enumBody
    )?
    ;

negativeProp
    :
    MINUS prop
    ;

userProp
    :
    (doc = DocComment)?
    (annotations += annotation)*
    prop COLON typeRef
    (
        EQUAL
        (defaultMinus = MINUS)?
        defaultValue = (BooleanLiteral | IntegerLiteral | StringLiteral | FloatingPointLiteral | NULL)
    )?
    ;

prop
    : Identifier
    ;

typeRef
    :
    qualifiedName
    (LESS_THAN genericArguments += genericArgument (COMMA genericArguments += genericArgument)? GREATER_THAN)?
    (optional = QUESTION_MARK)?
    ;

genericArgument
    :
    wildcard = MULTIPLY |
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
    subPredicates += andPredicate (OR subPredicates += andPredicate)*
    ;

andPredicate
    :
    subPredicates += atomPredicate (AND subPredicates += atomPredicate)*
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
    left = propPath
    (
        op = EQUAL right = propValue
        |
        op = DIAMOND right = propValue
        |
        op = NOT_EQUAL right = propValue
        |
        op = LESS_THAN right = propValue
        |
        op = LESS_THAN_EQUAL right = propValue
        |
        op = GREATER_THAN right = propValue
        |
        op = GREATER_THAN_EQUAL right = propValue
        |
        op = Identifier right = propValue
    )
    ;

nullityPredicate
    :
    propPath IS (not = NOT)? NULL
    ;

propPath
    :
    parts += Identifier (DOT parts += Identifier)*
    ;

propValue
    :
    booleanToken = BooleanLiteral |
    characterToken = CharacterLiteral |
    stringToken = SqlStringLiteral |
    (negative = MINUS)?  integerToken = IntegerLiteral |
    (negative = MINUS)?  floatingPointToken = FloatingPointLiteral |
    ;

orderBy
    :
    CONFIG_ORDER_BY LEFT_PARENTHESIS items += orderByItem (COMMA items += orderByItem)* RIGHT_PARENTHESIS
    ;

orderByItem
    :
    propPath (orderMode = Identifier)?
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
    CONFIG_FETCH_TYPE LEFT_PARENTHESIS fetchMode = Identifier RIGHT_PARENTHESIS
    ;

limit
    :
    CONFIG_LIMIT LEFT_PARENTHESIS limitArg = IntegerLiteral (COMMA offsetArg = IntegerLiteral)? RIGHT_PARENTHESIS
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
    AT typeName = qualifiedName (LEFT_PARENTHESIS annotationArguments? RIGHT_PARENTHESIS)?
    ;

annotationArguments
    :
    defaultArgument = annotationValue (COMMA namedArguments += annotationNamedArgument)*
    |
    namedArguments += annotationNamedArgument (COMMA namedArguments += annotationNamedArgument)*
    ;

annotationNamedArgument
    :
    name EQUAL value = annotationValue
    ;

annotationValue
    :
    annotationSingleValue
    |
    annotationArrayValue
    ;

annotationSingleValue
    :
    booleanToken = BooleanLiteral |
    characterToken = CharacterLiteral |
    stringTokens += StringLiteral (PLUS stringTokens += StringLiteral)* |
    (negative = MINUS)? integerToken = IntegerLiteral |
    (negative = MINUS)? floatingPointToken = FloatingPointLiteral |
    qualifiedPart = qualifiedName classSuffix? |
    annotationPart = annotation |
    nestedAnnotationPart = nestedAnnotation
    ;

annotationArrayValue
    :
    LEFT_BRACE elements += annotationSingleValue (COMMA elements += annotationSingleValue)* RIGHT_BRACE
    |
    LEFT_BRACKET elements += annotationSingleValue (COMMA elements += annotationSingleValue)* RIGHT_BRACKET
    ;

nestedAnnotation
    :
    typeName = qualifiedName LEFT_PARENTHESIS annotationArguments? RIGHT_PARENTHESIS
    ;

enumBody
    :
    LEFT_BRACE (mappings += enumMapping (COMMA|SEMICOLON)?)+ RIGHT_BRACE
    ;

enumMapping
    :
    constant = Identifier COLON
    (
        value = StringLiteral | (negative = MINUS)? value = IntegerLiteral
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

RIGHT_ARROR  : '->';
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
    '\'' ( ~'\'' | '\'\'' )* '\''
    ;

CharacterLiteral
	:
	'\'' SingleCharacter '\''
	|
	'\'' EscapeSequence '\''
	;

fragment
SingleCharacter
	:
	~['\\\r\n]
	;

StringLiteral
	:
	'"' StringCharacters? '"'
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