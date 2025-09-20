/**
 * Lexical Analyzer (Lexer) for a Custom Programming Language
 *
 * This lexer tokenizes source code and handles:
 * - Keywords: if, else, while, return, func
 * - Identifiers: team prefixes (134, 104, 199) + letter/underscore body
 * - Numbers: integers and floating-point with scientific notation
 * - Strings: enclosed in $ symbols, no newlines allowed
 * - Operators: arithmetic, comparison, assignment
 * - Comments: single-line (//) and multi-line ( ) with nesting
 * - Error handling and recovery
 **/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Enumeration of all possible token types in our language
 */
enum TokenType {
    IDENTIFIER,  // 134abc, 104_var, etc.
    KEYWORD,     // if, else, while, return, func
    INTEGER,     // 42, 0, 999
    NUMBER,      // 3.14, 2.5e10, .5
    STRING,      // $hello world$, $test$
    OPERATOR,    // +, -, ==, !=, etc.
    ERROR,       // malformed tokens, unknown symbols
    EOF          // end of input
}

/**
 * Represents a single token with its type, text content, and position
 */
class Token {
    final TokenType type;    // What kind of token this is
    final String lexeme;     // The actual text content
    final int line, col;     // Position in source code

    Token(TokenType type, String lexeme, int line, int col) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.col = col;
    }

    @Override
    public String toString() {
        return type + "('" + lexeme + "')@" + line + ":" + col;
    }
}

/**
 * The main lexical analyzer class
 * Converts source code string into a sequence of tokens
 */
class Lexer {
    private final String src;           // Source code to tokenize
    private int i = 0;                  // Current position in source
    private int line = 1, col = 1;     // Current line and column for error reporting

    // Set of reserved keywords in our language
    private static final Set<String> KEYWORDS = Set.of("if", "else", "while", "return", "func");

    /**
     * Constructor - initialize lexer with source code
     */
    Lexer(String input) {
        this.src = input;
    }

    // =================================
    // UTILITY METHODS FOR NAVIGATION
    // =================================

    /**
     * Check if we've reached the end of input
     */
    private boolean eof() {
        return i >= src.length();
    }

    /**
     * Look at current character without consuming it
     */
    private char peek() {
        return eof() ? '\0' : src.charAt(i);
    }

    /**
     * Look ahead k characters without consuming
     */
    private char peek(int k) {
        int j = i + k;
        return j >= src.length() ? '\0' : src.charAt(j);
    }

    /**
     * Consume and return current character, updating position
     */
    private char next() {
        char c = peek();
        if (c == '\0') return c;

        i++;
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    // =================================
    // CHARACTER CLASSIFICATION METHODS
    // =================================

    private boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isLetterOrUnderscore(char c) {
        return isLetter(c) || c == '_';
    }

    private boolean isLetterDigitUnderscore(char c) {
        return isLetter(c) || isDigit(c) || c == '_';
    }

    /**
     * Skip whitespace characters (space, tab, carriage return, newline)
     */
    private void skipWhitespace() {
        while (!eof()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                next();
            } else {
                break;
            }
        }
    }

    // =================================
    // MAIN TOKENIZATION METHOD
    // =================================

    /**
     * Extract and return the next token from input
     * This is the main entry point for tokenization
     */
    public Token nextToken() {
        skipWhitespace();
        if (eof()) return new Token(TokenType.EOF, "<EOF>", line, col);

        char c = peek();
        int startLine = line, startCol = col;

        // Dispatch to appropriate scanner based on first character
        if (c == '/') return scanSlashOrComment();
        if (c == '$') return scanString(startLine, startCol);
        if (c == '1') {
            // Try identifier first (team prefixes), fall back to number
            Token id = tryScanIdentifier(startLine, startCol);
            if (id != null) return id;
            return scanNumber(startLine, startCol);
        }
        if (isLetter(c)) return scanKeywordOrError(startLine, startCol);
        if (isDigit(c) || c == '.') return scanNumber(startLine, startCol);
        if ("+-=*<>!".indexOf(c) >= 0) return scanOperator(startLine, startCol);

        // Unknown character - consume it and return error
        next();
        return new Token(TokenType.ERROR, String.valueOf(c), startLine, startCol);
    }

    // =================================
    // OPERATOR SCANNING
    // =================================

    /**
     * Scan operators including compound ones (==, !=, <=, >=, <>)
     */
    private Token scanOperator(int startLine, int startCol) {
        char c = next();
        char n = peek();
        String op = String.valueOf(c);

        // Handle compound operators
        if (c == '=') {
            if (n == '=') {
                next();
                op = "==";
            }
        } else if (c == '!') {
            if (n == '=') {
                next();
                op = "!=";
            } else {
                // Standalone '!' is an error in our language
                return new Token(TokenType.ERROR, "!", startLine, startCol);
            }
        } else if (c == '<') {
            if (n == '=') {
                next();
                op = "<=";
            } else if (n == '>') {
                next();
                op = "<>";
            }
        } else if (c == '>') {
            if (n == '=') {
                next();
                op = ">=";
            }
        }

        return new Token(TokenType.OPERATOR, op, startLine, startCol);
    }

    // =================================
    // COMMENT SCANNING
    // =================================

    /**
     * Handle '/' which could be division, single-line comment, or multi-line comment
     */
    private Token scanSlashOrComment() {
        int startLine = line, startCol = col;
        next(); // consume '/'
        char n = peek();

        if (n == '/') {
            // Single-line comment: skip until newline
            next(); // consume second '/'
            while (!eof() && peek() != '\n') next();
            if (!eof() && peek() == '\n') next(); // consume newline
            return nextToken(); // recursively get next token
        } else if (n == '*') {
            // Multi-line comment with nesting support
            next(); // consume '*'
            int nestingLevel = 1;

            while (!eof()) {
                char c = next();
                if (c == '/' && peek() == '*') {
                    next(); // consume '*'
                    nestingLevel++;
                } else if (c == '*') {
                    // Skip multiple consecutive '*' characters
                    while (peek() == '*') next();
                    if (peek() == '/') {
                        next(); // consume '/'
                        nestingLevel--;
                        if (nestingLevel == 0) break;
                    }
                }
            }

            if (nestingLevel != 0) {
                return new Token(TokenType.ERROR, "unterminated comment", startLine, startCol);
            }
            return nextToken(); // recursively get next token
        } else {
            // Just a division operator
            return new Token(TokenType.OPERATOR, "/", startLine, startCol);
        }
    }

    // =================================
    // STRING SCANNING
    // =================================

    /**
     * Scan strings enclosed in $ symbols
     * Strings can contain any character except newlines and \n escape sequence
     */
    private Token scanString(int startLine, int startCol) {
        next(); // consume opening '$'
        StringBuilder sb = new StringBuilder();

        while (!eof()) {
            char c = next();

            if (c == '$') {
                // Found closing delimiter - return successful string token
                return new Token(TokenType.STRING, sb.toString(), startLine, startCol);
            }

            // Check for forbidden \n escape sequence
            if (c == '\\') {
                if (peek() == 'n') {
                    next(); // consume 'n'
                    // Error recovery: skip until next '$' or newline
                    while (!eof() && peek() != '$' && peek() != '\n') next();
                    if (!eof() && peek() == '$') next();
                    return new Token(TokenType.ERROR, "string contains forbidden \\n", startLine, startCol);
                } else {
                    // Other backslashes are treated as literal characters
                    sb.append(c);
                    continue;
                }
            }

            // Real newline inside string is an error
            if (c == '\n' || c == '\0') {
                // Error recovery: skip until next '$' delimiter
                while (!eof() && peek() != '$') {
                    if (peek() == '\n') break; // stop at another newline
                    next();
                }
                if (!eof() && peek() == '$') next();
                return new Token(TokenType.ERROR, "unterminated string", startLine, startCol);
            }

            // Add character to string content (including spaces and all other valid chars)
            sb.append(c);
        }

        // Reached EOF without closing delimiter
        return new Token(TokenType.ERROR, "unterminated string", startLine, startCol);
    }

    // =================================
    // KEYWORD SCANNING
    // =================================

    /**
     * Scan words that start with letters - they're either keywords or errors
     * (Regular identifiers must start with team prefixes)
     */
    private Token scanKeywordOrError(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();

        // Consume consecutive letters
        while (!eof() && isLetter(peek())) {
            sb.append(next());
        }

        String word = sb.toString();
        if (KEYWORDS.contains(word)) {
            return new Token(TokenType.KEYWORD, word, startLine, startCol);
        }

        return new Token(TokenType.ERROR, "unknown word: " + word, startLine, startCol);
    }

    // =================================
    // IDENTIFIER SCANNING
    // =================================

    /**
     * Try to scan identifiers that start with team prefixes (134, 104, 199)
     * Returns null if no valid identifier found (letting number scanner handle it)
     */
    private Token tryScanIdentifier(int startLine, int startCol) {
        // Save current state for potential rollback
        int savedPos = i, savedLine = line, savedCol = col;

        if (peek() != '1') return null;

        // Check for valid team prefixes
        if (peek(1) == '3' && peek(2) == '4') {
            next(); next(); next(); // consume "134"
        } else if (peek(1) == '0' && peek(2) == '4') {
            next(); next(); next(); // consume "104"
        } else if (peek(1) == '9' && peek(2) == '9') {
            next(); next(); next(); // consume "199"
        } else {
            return null; // Not a team prefix
        }

        // Check if there's a valid identifier body (letter or underscore)
        if (!isLetterOrUnderscore(peek())) {
            // Team prefix found but no valid identifier body
            // Rollback and let number scanner handle it as INTEGER
            i = savedPos;
            line = savedLine;
            col = savedCol;
            return null;
        }

        // Valid identifier: consume the rest of the body
        while (!eof() && isLetterDigitUnderscore(peek())) {
            next();
        }

        String lexeme = src.substring(savedPos, i);
        return new Token(TokenType.IDENTIFIER, lexeme, startLine, startCol);
    }

    // =================================
    // NUMBER SCANNING
    // =================================

    /**
     * Scan numeric literals: integers, floats, and scientific notation
     * Handles complex cases like .5, 12.34e-10, malformed numbers, etc.
     */
    private Token scanNumber(int startLine, int startCol) {
        int startPos = i;
        boolean seenInt = false, seenDot = false, seenFrac = false, seenExp = false;

        // Handle numbers starting with decimal point (.5, .25e10)
        if (peek() == '.') {
            seenDot = true;
            next(); // consume '.'

            if (isDigit(peek())) {
                // Valid: .123
                while (isDigit(peek())) {
                    next();
                    seenFrac = true;
                }
            } else if (peek() == 'e' || peek() == 'E') {
                // Invalid: .e10 (no fractional part)
                next(); // consume 'e'/'E'
                if (peek() == '+' || peek() == '-') next();
                while (isDigit(peek())) next();
                String badToken = src.substring(startPos, i);
                return new Token(TokenType.ERROR, "bad exponent: " + badToken, startLine, startCol);
            } else {
                // Just a lone dot - treat as error
                i = startPos;
                char dot = next();
                return new Token(TokenType.ERROR, String.valueOf(dot), startLine, startCol);
            }
        } else {
            // Handle numbers starting with digits
            while (isDigit(peek())) {
                next();
                seenInt = true;
            }

            // Check for decimal point
            if (peek() == '.') {
                seenDot = true;
                next();

                if (peek() == '.') {
                    // Malformed: 12..3
                    next();
                    while (isDigit(peek())) next();
                    String badToken = src.substring(startPos, i);
                    return new Token(TokenType.ERROR, "malformed number: " + badToken, startLine, startCol);
                }

                // Consume fractional digits
                while (isDigit(peek())) {
                    next();
                    seenFrac = true;
                }
            }
        }

        // Handle scientific notation (e or E)
        if (peek() == 'e' || peek() == 'E') {
            // Check for malformed float before exponent (e.g., 10.e5)
            if (seenDot && !seenFrac) {
                next(); // consume 'e'/'E'
                if (peek() == '+' || peek() == '-') next();
                while (isDigit(peek())) next();
                String badToken = src.substring(startPos, i);
                return new Token(TokenType.ERROR, "malformed float: " + badToken, startLine, startCol);
            }

            seenExp = true;
            next(); // consume 'e'/'E'

            // Optional sign
            if (peek() == '+' || peek() == '-') next();

            // Must have digits after e/E
            if (!isDigit(peek())) {
                if (isLetter(peek())) next(); // consume the invalid character
                String badToken = src.substring(startPos, Math.max(i, startPos + 1));
                return new Token(TokenType.ERROR, "bad exponent: " + badToken, startLine, startCol);
            }

            while (isDigit(peek())) next();
        }

        String lexeme = src.substring(startPos, i);

        // Final validation
        if (seenDot && !seenFrac) {
            return new Token(TokenType.ERROR, "malformed float: " + lexeme, startLine, startCol);
        }

        // Determine token type
        if (seenDot || seenExp) {
            return new Token(TokenType.NUMBER, lexeme, startLine, startCol);
        }
        if (seenInt) {
            return new Token(TokenType.INTEGER, lexeme, startLine, startCol);
        }

        // Fallback error
        return new Token(TokenType.ERROR, src.substring(startPos, Math.min(startPos + 1, src.length())), startLine, startCol);
    }
}

/**
 * Main class - handles input/output and drives the lexical analysis
 */
public class Main {

    /**
     * Read input from standard input (console)
     * User types lines and ends with a single line containing "END"
     */
    private static String readFromStdin() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter source code (type 'END' on a separate line to finish):");
        StringBuilder content = new StringBuilder();

        while (true) {
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine();
            if (line.equals("END")) break;
            content.append(line).append('\n');
        }

        return content.toString();
    }

    /**
     * Read input from a file
     * If file reading fails, falls back to standard input
     */
    private static String readFromFile(String filePath) {
        try {
            return Files.readString(Path.of(filePath));
        } catch (IOException e) {
            System.err.println("Error reading file '" + filePath + "': " + e.getMessage());
            return null; // Return null instead of falling back automatically
        }
    }

    /**
     * Print usage instructions for the program
     */
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java Main                    # Interactive mode - choose input type");
        System.out.println("  java Main <filename>         # Direct file input");
        System.out.println("  java Main -f <filename>      # Direct file input (explicit flag)");
        System.out.println("  java Main -c                 # Direct console input");
    }

    /**
     * Interactive method to let user choose input type
     */
    private static String chooseInputSource() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Choose input source:");
            System.out.println("1. Console input (type code directly)");
            System.out.println("2. File input (read from file)");
            System.out.print("Enter your choice (1 or 2): ");

            if (!scanner.hasNextLine()) {
                System.err.println("No input received. Exiting.");
                System.exit(1);
            }

            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                // Console input chosen
                return readFromStdin();
            } else if (choice.equals("2")) {
                // File input chosen
                System.out.print("Enter file path: ");
                if (!scanner.hasNextLine()) {
                    System.err.println("No file path provided. Exiting.");
                    System.exit(1);
                }
                String filePath = scanner.nextLine().trim();

                String content = readFromFile(filePath);
                if (content == null) {
                    System.out.println("Would you like to try again or switch to console input?");
                    System.out.println("1. Try another file");
                    System.out.println("2. Switch to console input");
                    System.out.print("Choice (1 or 2): ");

                    if (!scanner.hasNextLine()) {
                        System.err.println("No choice provided. Exiting.");
                        System.exit(1);
                    }

                    String retry = scanner.nextLine().trim();
                    if (retry.equals("2")) {
                        return readFromStdin();
                    }
                    // Continue loop to try another file
                    continue;
                }
                return content;
            } else {
                System.out.println("Invalid choice. Please enter 1 or 2.");
            }
        }
    }

    /**
     * Main entry point
     * Handles command-line arguments, reads input, tokenizes, and displays results
     */
    public static void main(String[] args) {
        String input;

        // Parse command-line arguments for input source
        if (args.length == 0) {
            // No arguments: interactive mode - let user choose
            input = chooseInputSource();
        } else if (args.length == 1) {
            if (args[0].equals("-h") || args[0].equals("--help")) {
                printUsage();
                return;
            } else if (args[0].equals("-c")) {
                // Direct console input
                input = readFromStdin();
            } else {
                // Single argument: treat as filename
                input = readFromFile(args[0]);
                if (input == null) {
                    System.err.println("Failed to read file. Exiting.");
                    return;
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("-f")) {
            // Two arguments with -f flag: read from specified file
            input = readFromFile(args[1]);
            if (input == null) {
                System.err.println("Failed to read file. Exiting.");
                return;
            }
        } else {
            // Invalid arguments
            System.err.println("Invalid arguments.");
            printUsage();
            return;
        }

        // Create lexer and tokenize the input
        Lexer lexer = new Lexer(input);
        List<Token> tokens = new ArrayList<>();

        // Extract all tokens until EOF
        while (true) {
            Token token = lexer.nextToken();
            tokens.add(token);
            if (token.type == TokenType.EOF) break;
        }

        // Display results
        System.out.println("=== TOKENS ===");
        for (Token token : tokens) {
            System.out.println(token);
        }
    }
}
