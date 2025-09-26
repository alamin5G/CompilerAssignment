# Lexical Analyzer Program Guide

## 1. Introduction

This lexical analyzer (lexer) is a Java program designed to tokenize source code for a custom programming language. The program reads input source code and breaks it down into a sequence of tokens, which are the smallest meaningful units in the language. These tokens can then be processed by a parser as part of a compiler or interpreter.

The lexer handles various token types including:

- Keywords (if, else, while, return, func)
- Identifiers with team prefixes (134, 104, 199)
- Numbers (integers and floating-point with scientific notation)
- Strings enclosed in $ symbols
- Operators (arithmetic, comparison, assignment)
- Comments (single-line and multi-line with nesting support)
- Error handling for malformed tokens

## 2. Program Workflow and Architecture

The program follows a clear workflow:

1. **Input Acquisition**: The program can read input from either the console or a file, with multiple options for specifying the input source.
2. **Lexical Analysis**: The input is processed character by character to identify and generate tokens.
3. **Token Output**: The generated tokens are displayed to the user, showing their type, lexeme, and position in the source code.

The architecture consists of four main components:

- **TokenType**: An enumeration defining all possible token types
- **Token**: A class representing individual tokens with their type, lexeme, and position
- **Lexer**: The main lexical analysis class that converts source code into tokens
- **Main**: The entry point class that handles input/output and drives the lexical analysis

## 3. Main Components and Classes

### TokenType Enum

The [`TokenType`](src/Main.java:26) enumeration defines all possible token types in the language:

```java
enum TokenType {
    IDENTIFIER, // Team-prefixed identifiers like 134abc, 104_var, etc.
    KEYWORD,    // Reserved words: if, else, while, return, func
    INTEGER,    // Whole numbers like 42, 0, 999
    NUMBER,     // Floating-point numbers with optional exponent like 3.14, 2.5e10, .5
    STRING,     // Text enclosed in $ symbols like $hello world$, $test$
    OPERATOR,   // Mathematical and comparison operators like +, -, ==, !=, etc.
    ERROR,      // Represents malformed tokens or unknown symbols
    EOF         // End of file/input marker
}
```

### Token Class

The [`Token`](src/Main.java:40) class represents individual tokens with their type, lexeme, and position:

```java
class Token {
    final TokenType type;   // Enum value indicating what kind of token this is
    final String lexeme;   // The actual text content of the token as it appears in source
    final int line, col;   // Position in source code for error reporting (line and column)

    // Constructor to create a new Token with specified properties
    Token(TokenType type, String lexeme, int line, int col) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.col = col;
    }

    // Override toString to provide a readable representation of the token
    @Override
    public String toString() {
        // Format: TYPE('lexeme')@line:col
        return type + "('" + lexeme + "')@" + line + ":" + col;
    }
}
```

### Lexer Class

The [`Lexer`](src/Main.java:65) class is the core component that performs lexical analysis:

```java
class Lexer {
    private final String src;    // Source code to tokenize (immutable)
    private int i = 0;           // Current position/index in source string
    private int line = 1, col = 1; // Current line and column for error reporting

    // Set of reserved keywords in our language for quick lookup
    private static final Set<String> KEYWORDS = Set.of("if", "else", "while", "return", "func");

    // Constructor - initialize lexer with source code
    Lexer(String input) {
        this.src = input;
    }

    // ... methods for tokenization ...
}
```

### Main Class

The [`Main`](src/Main.java:639) class handles input/output and drives the lexical analysis:

```java
public class Main {
    // Methods for reading input from stdin or file
    private static String readFromStdin() { ... }
    private static String readFromFile(String filePath) { ... }

    // Method for interactive input source selection
    private static String chooseInputSource() { ... }

    // Main entry point
    public static void main(String[] args) { ... }
}
```

## 4. Key Methods and Their Purposes

### Lexer Class Methods

#### Navigation Methods

- [`eof()`](src/Main.java:87): Checks if the lexer has reached the end of input
- [`peek()`](src/Main.java:95): Looks at the current character without consuming it
- [`peek(int k)`](src/Main.java:103): Looks ahead k characters without consuming them
- [`next()`](src/Main.java:114): Consumes and returns the current character, updating position tracking

#### Character Classification Methods

- [`isLetter(char c)`](src/Main.java:139): Checks if a character is an uppercase or lowercase letter
- [`isDigit(char c)`](src/Main.java:144): Checks if a character is a digit (0-9)
- [`isLetterOrUnderscore(char c)`](src/Main.java:150): Checks if a character is a letter or underscore
- [`isLetterDigitUnderscore(char c)`](src/Main.java:156): Checks if a character is a letter, digit, or underscore

#### Tokenization Methods

- [`skipWhitespace()`](src/Main.java:163): Skips whitespace characters (space, tab, carriage return, newline)
- [`nextToken()`](src/Main.java:185): Extracts and returns the next token from input (main entry point)
- [`scanOperator(int startLine, int startCol)`](src/Main.java:243): Scans operators including compound ones (==, !=, <=, >=, <>)
- [`scanSlashOrComment()`](src/Main.java:300): Handles '/' which could be division, single-line comment, or multi-line comment
- [`scanString(int startLine, int startCol)`](src/Main.java:371): Scans strings enclosed in $ symbols
- [`scanKeywordOrError(int startLine, int startCol)`](src/Main.java:433): Scans words that start with letters (either keywords or errors)
- [`tryScanIdentifier(int startLine, int startCol)`](src/Main.java:462): Tries to scan identifiers that start with team prefixes (134, 104, 199)
- [`scanNumber(int startLine, int startCol)`](src/Main.java:517): Scans numeric literals: integers, floats, and scientific notation

### Main Class Methods

- [`readFromStdin()`](src/Main.java:645): Reads input from standard input (console)
- [`readFromFile(String filePath)`](src/Main.java:675): Reads input from a file
- [`printUsage()`](src/Main.java:690): Prints usage instructions for the program
- [`chooseInputSource()`](src/Main.java:703): Interactive method to let user choose input type
- [`main(String[] args)`](src/Main.java:779): Main entry point that handles command-line arguments, reads input, tokenizes, and displays results

## 5. Token Types and Recognition

### Identifiers

- **Format**: Must start with one of the team prefixes (134, 104, 199) followed by a letter or underscore, then any combination of letters, digits, and underscores.
- **Examples**: `134Alamin`, `104Ritu`, `199ABC123`, `104_user1`
- **Recognition**: The [`tryScanIdentifier()`](src/Main.java:462) method first checks for valid team prefixes, then validates the identifier body.

### Keywords

- **Format**: Reserved words in the language: `if`, `else`, `while`, `return`, `func`.
- **Examples**: `if`, `else`, `while`
- **Recognition**: The [`scanKeywordOrError()`](src/Main.java:433) method scans words that start with letters and checks if they match any of the reserved keywords.

### Numbers

- **Integer Format**: One or more digits (0-9), with optional leading zeros.
- **Floating-point Format**: Integer part, decimal point, and fractional part; or decimal point followed by fractional part.
- **Scientific Notation**: Integer or floating-point number followed by 'e' or 'E', optional sign (+ or -), and exponent digits.
- **Examples**: `42`, `007`, `10.5`, `.5`, `10E15`, `10e-10`, `3.14159e+0`, `.25E2`
- **Recognition**: The [`scanNumber()`](src/Main.java:517) method handles complex cases including numbers starting with decimal points, malformed numbers, and scientific notation.

### Strings

- **Format**: Text enclosed in $ symbols. Can contain any character except newlines and the literal `\n` escape sequence.
- **Examples**: `$hello world$`, `$test$`, `$$` (empty string)
- **Recognition**: The [`scanString()`](src/Main.java:371) method processes characters until a closing $ is found, with error handling for forbidden escape sequences and newlines.

### Operators

- **Format**: Single or compound characters for arithmetic, comparison, and assignment operations.
- **Examples**: `+`, `-`, `*`, `/`, `=`, `==`, `!=`, `<=`, `>=`, `<`, `>`, `<>`
- **Recognition**: The [`scanOperator()`](src/Main.java:243) method handles both single and compound operators.

### Comments

- **Single-line Format**: Starts with // and continues to the end of the line.
- **Multi-line Format**: Starts with /_ and ends with _/, with support for nested comments.
- **Examples**: `// single line comment`, `/* multi-line comment */`, `/* nested /* comment */ */`
- **Recognition**: The [`scanSlashOrComment()`](src/Main.java:300) method handles both types of comments, with special support for nested multi-line comments.

### Error Tokens

- **Format**: Any malformed or unrecognized tokens.
- **Examples**: `134` (prefix without body), `_abc` (invalid identifier start), `5.` (malformed float)
- **Recognition**: Various scanning methods return error tokens when they encounter malformed input.

## 6. Input Handling

### Console Input

The program can read input directly from the console through the [`readFromStdin()`](src/Main.java:645) method:

- User types lines of source code
- Input ends when the user types "END" on a separate line
- All lines are collected into a single string for processing

### File Input

The program can read input from files through the [`readFromFile()`](src/Main.java:675) method:

- Takes a file path as input
- Uses Java NIO's Files.readString() to read the entire file content
- Returns null if file reading fails, allowing the caller to handle the error

### Command-line Arguments

The program supports multiple ways to specify input source through command-line arguments:

- `java Main`: Interactive mode - user chooses input type
- `java Main <filename>`: Direct file input
- `java Main -f <filename>`: Direct file input with explicit flag
- `java Main -c`: Direct console input
- `java Main -h` or `java Main --help`: Display usage instructions

### Interactive Input Selection

When no arguments are provided, the [`chooseInputSource()`](src/Main.java:703) method presents an interactive menu:

1. Console input (type code directly)
2. File input (read from file)

If file input fails, the user is given options to try another file or switch to console input.

## 7. Error Handling Mechanisms

The lexer implements comprehensive error handling for various scenarios:

### Malformed Tokens

- **Identifiers**: Returns error tokens for identifiers without valid team prefixes or without a proper body
- **Numbers**: Handles malformed numbers like `5.`, `.e5`, `10.e5`, `12..3`, `1e+`
- **Strings**: Detects unterminated strings, newlines within strings, and forbidden `\n` escape sequences
- **Comments**: Identifies unterminated multi-line comments

### Error Recovery

The lexer attempts to recover from errors when possible:

- **Strings**: When encountering a forbidden `\n` or newline, the lexer skips ahead to find the next `$` delimiter
- **Comments**: For single-line comments, the lexer simply skips to the end of the line
- **General**: After detecting an error, the lexer continues scanning subsequent tokens

### Error Reporting

Error tokens include:

- The type `TokenType.ERROR`
- The problematic lexeme or a descriptive error message
- The line and column position where the error occurred

This information helps users identify and fix issues in their source code.

## 8. Examples of Program Usage

### Basic Usage Examples

#### Interactive Mode

```
java Main
Choose input source:
1. Console input (type code directly)
2. File input (read from file)
Enter your choice (1 or 2): 1
Enter source code (type 'END' on a separate line to finish):
134Alamin = 10.5 + .5
END
=== TOKENS ===
IDENTIFIER('134Alamin')@1:1
OPERATOR('=')@1:10
NUMBER('10.5')@1:12
OPERATOR('+')@1:17
NUMBER('.5')@1:19
EOF('<EOF>')@2:1
```

#### File Input

```
java Main test_input.txt
=== TOKENS ===
IDENTIFIER('134Alamin')@1:1
KEYWORD('func')@2:1
IDENTIFIER('test')@2:6
ERROR('(')@2:10
ERROR(')')@2:11
ERROR('{')@2:13
KEYWORD('if')@3:5
ERROR('x')@3:8
OPERATOR('>')@3:10
INTEGER('10')@3:12
ERROR('{')@3:15
KEYWORD('return')@4:9
STRING('hello world')@4:16
ERROR('}')@4:29
ERROR('}')@5:5
EOF('<EOF>')@7:1
```

#### Direct Console Input

```
java Main -c
Enter source code (type 'END' on a separate line to finish):
if 134_Alqmin <> 104Ritu
END
=== TOKENS ===
KEYWORD('if')@1:1
IDENTIFIER('134_Alqmin')@1:4
OPERATOR('<>')@1:15
IDENTIFIER('104Ritu')@1:18
EOF('<EOF>')@2:1
```

### Test Case Examples

The program includes comprehensive test cases covering all token types and error scenarios. Here are some examples:

#### Valid Identifiers

- Input: `134Alamin`
- Output: `IDENTIFIER('134Alamin')@1:1`

#### Invalid Identifiers

- Input: `134`
- Output: `ERROR('1')@1:1` (or similar error token)

#### Keywords

- Input: `if else while return func`
- Output:
  ```
  KEYWORD('if')@1:1
  KEYWORD('else')@1:4
  KEYWORD('while')@1:9
  KEYWORD('return')@1:15
  KEYWORD('func')@1:22
  ```

#### Numbers

- Input: `10.5 .5 10E15 10e-10`
- Output:
  ```
  NUMBER('10.5')@1:1
  NUMBER('.5')@1:6
  NUMBER('10E15')@1:9
  NUMBER('10e-10')@1:15
  ```

#### Strings

- Input: `$hello world$`
- Output: `STRING('hello world')@1:1`

#### Operators

- Input: `+ - * / = == != <= >= < > <>`
- Output:
  ```
  OPERATOR('+')@1:1
  OPERATOR('-')@1:3
  OPERATOR('*')@1:5
  OPERATOR('/')@1:7
  OPERATOR('=')@1:9
  OPERATOR('==')@1:11
  OPERATOR('!=')@1:14
  OPERATOR('<=')@1:17
  OPERATOR('>=')@1:20
  OPERATOR('<')@1:23
  OPERATOR('>')@1:25
  OPERATOR('<>')@1:27
  ```

#### Comments

- Input: `// single line comment`
- Output: (no tokens, comment is skipped)

- Input: `/* multi-line comment */`
- Output: (no tokens, comment is skipped)

#### Error Cases

- Input: `5.`
- Output: `ERROR('malformed float: 5.')@1:1`

- Input: `$unterminated`
- Output: `ERROR('unterminated string')@1:1`

## 9. Additional Information

### Design Decisions

1. **Team Prefixes for Identifiers**: Identifiers must start with specific team prefixes (134, 104, 199), which is an unusual requirement for most programming languages but serves as a specific constraint for this lexer.

2. **String Delimiters**: Strings use `$` as delimiters instead of the more common double quotes, which requires special handling in the lexer.

3. **Comment Nesting**: Multi-line comments support nesting, which is not a common feature in most languages but adds complexity to the lexer implementation.

4. **Error Handling**: The lexer attempts to recover from errors when possible, allowing it to continue processing subsequent tokens rather than stopping at the first error.

### Limitations

1. **Unsupported Characters**: Characters not specified in the language definition (like parentheses, braces, brackets, semicolons) are treated as errors.

2. **String Limitations**: Strings cannot span multiple lines and cannot contain the literal `\n` escape sequence.

3. **Number Limitations**: Numbers must have proper formatting; for example, a decimal point must be followed by digits.

### Testing

The program includes comprehensive test cases in the [`src/test-cases.md`](src/test-cases.md) file, covering:

- Valid and invalid identifiers
- Keywords
- Valid and malformed numbers
- Valid and invalid strings
- Operators
- Comments (single-line and multi-line)
- Mixed sequences
- Boundary/whitespace cases
- Unknown/unsupported symbols
- Edge cases for identifiers and exponents

### Extending the Lexer

To extend the lexer for additional language features:

1. Add new token types to the `TokenType` enum
2. Update the `nextToken()` method to recognize new patterns
3. Implement new scanning methods or modify existing ones
4. Update the `KEYWORDS` set if adding new reserved words
5. Add comprehensive test cases for the new features

This lexical analyzer provides a solid foundation for building a compiler or interpreter for the custom language, with robust error handling and comprehensive token recognition capabilities.
