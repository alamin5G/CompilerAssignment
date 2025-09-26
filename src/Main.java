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

// Import Java I/O classes for file reading and exception handling
import java.io.IOException;
// Import Java NIO classes for modern file operations
import java.nio.file.Files;
// Import Path class for representing file paths in a filesystem
import java.nio.file.Path;
// Import Java utility classes for collections like Set and List
import java.util.*;

/**
 * Enumeration of all possible token types in our language
 */
enum TokenType {
    IDENTIFIER, // Team-prefixed identifiers like 134abc, 104_var, etc.
    KEYWORD, // Reserved words: if, else, while, return, func
    INTEGER, // Whole numbers like 42, 0, 999
    NUMBER, // Floating-point numbers with optional exponent like 3.14, 2.5e10, .5
    STRING, // Text enclosed in $ symbols like $hello world$, $test$
    OPERATOR, // Mathematical and comparison operators like +, -, ==, !=, etc.
    ERROR, // Represents malformed tokens or unknown symbols
    EOF // End of file/input marker
}

/**
 * Represents a single token with its type, text content, and position
 */
class Token {
    final TokenType type; // Enum value indicating what kind of token this is
    final String lexeme; // The actual text content of the token as it appears in source
    final int line, col; // Position in source code for error reporting (line and column)

    // Constructor to create a new Token with specified properties
    Token(TokenType type, String lexeme, int line, int col) {
        this.type = type; // Set the token type
        this.lexeme = lexeme; // Set the actual text content
        this.line = line; // Set the line number where token starts
        this.col = col; // Set the column number where token starts
    }

    // Override toString to provide a readable representation of the token
    @Override
    public String toString() {
        // Format: TYPE('lexeme')@line:col
        return type + "('" + lexeme + "')@" + line + ":" + col;
    }
}

/**
 * The main lexical analyzer class
 * Converts source code string into a sequence of tokens
 */
class Lexer {
    private final String src; // Source code to tokenize (immutable)
    private int i = 0; // Current position/index in source string
    private int line = 1, col = 1; // Current line and column for error reporting and token positioning

    // Set of reserved keywords in our language for quick lookup
    private static final Set<String> KEYWORDS = Set.of("if", "else", "while", "return", "func");

    /**
     * Constructor - initialize lexer with source code
     */
    Lexer(String input) {
        this.src = input; // Store the source code to be tokenized
    }

    // =================================
    // UTILITY METHODS FOR NAVIGATION
    // =================================

    /**
     * Check if we've reached the end of input
     */
    private boolean eof() {
        // Return true if current position is at or beyond the end of source string
        return i >= src.length();
    }

    /**
     * Look at current character without consuming it (peek operation)
     */
    private char peek() {
        // If at end of file, return null character, otherwise return current character
        return eof() ? '\0' : src.charAt(i);
    }

    /**
     * Look ahead k characters without consuming them
     */
    private char peek(int k) {
        // Calculate position k characters ahead
        int j = i + k;
        // If beyond end of string, return null character, otherwise return character at
        // that position
        return j >= src.length() ? '\0' : src.charAt(j);
    }

    /**
     * Consume and return current character, updating position tracking
     */
    private char next() {
        // Get current character without advancing
        char c = peek();
        // If we're at end of file, return null character
        if (c == '\0')
            return c;

        // Advance current position
        i++;
        // If character is newline, increment line count and reset column
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            // Otherwise just increment column count
            col++;
        }
        return c; // Return the consumed character
    }

    // =================================
    // CHARACTER CLASSIFICATION METHODS
    // =================================

    // Helper method to check if a character is an uppercase or lowercase letter
    private boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    // Helper method to check if a character is a digit (0-9)
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // Helper method to check if a character is a letter or underscore
    // Used for validating the start of identifier bodies after team prefixes
    private boolean isLetterOrUnderscore(char c) {
        return isLetter(c) || c == '_';
    }

    // Helper method to check if a character is a letter, digit, or underscore
    // Used for validating characters within identifier bodies
    private boolean isLetterDigitUnderscore(char c) {
        return isLetter(c) || isDigit(c) || c == '_';
    }

    /**
     * Skip whitespace characters (space, tab, carriage return, newline)
     */
    private void skipWhitespace() {
        // Continue until we reach end of file
        while (!eof()) {
            char c = peek(); // Look at current character
            // If it's any kind of whitespace, consume it
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                next();
            } else {
                // If not whitespace, we're done skipping
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
        // First, skip any whitespace characters to get to the next token
        skipWhitespace();
        // If we've reached the end of input, return EOF token
        if (eof())
            return new Token(TokenType.EOF, "<EOF>", line, col);

        // Get the current character and save the starting position
        char c = peek();
        int startLine = line, startCol = col;

        // Dispatch to appropriate scanner based on first character
        // This is the main tokenization logic that determines token type

        // If character is '/', it could be division or start of a comment
        if (c == '/')
            return scanSlashOrComment();

        // If character is '$', it's the start of a string literal
        if (c == '$')
            return scanString(startLine, startCol);

        // If character is '1', it could be start of a team-prefixed identifier or a
        // number
        if (c == '1') {
            // Try to scan as identifier first (team prefixes), fall back to number if not
            // valid
            Token id = tryScanIdentifier(startLine, startCol);
            if (id != null)
                return id;
            return scanNumber(startLine, startCol);
        }

        // If character is a letter, it could be a keyword or an error
        // (Regular identifiers must start with team prefixes)
        if (isLetter(c))
            return scanKeywordOrError(startLine, startCol);

        // If character is a digit or decimal point, it's a number
        if (isDigit(c) || c == '.')
            return scanNumber(startLine, startCol);

        // If character is one of the operator symbols, scan as operator
        if ("+-=*<>!".indexOf(c) >= 0)
            return scanOperator(startLine, startCol);

        // If we get here, it's an unknown character - consume it and return error token
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
        // Consume the first character of the operator
        char c = next();
        // Look at the next character to check if it's a compound operator
        char n = peek();
        // Start with the single character operator
        String op = String.valueOf(c);

        // Handle compound operators by checking the next character

        // If first character is '=', check if it's followed by another '=' to make "=="
        if (c == '=') {
            if (n == '=') {
                next(); // Consume the second '='
                op = "=="; // Update to compound operator
            }
        }
        // If first character is '!', check if it's followed by '=' to make "!="
        else if (c == '!') {
            if (n == '=') {
                next(); // Consume the '='
                op = "!="; // Update to compound operator
            } else {
                // Standalone '!' is an error in our language
                return new Token(TokenType.ERROR, "!", startLine, startCol);
            }
        }
        // If first character is '<', check for '<=' or '<>' compounds
        else if (c == '<') {
            if (n == '=') {
                next(); // Consume the '='
                op = "<="; // Update to less-than-or-equal operator
            } else if (n == '>') {
                next(); // Consume the '>'
                op = "<>"; // Update to not-equal operator
            }
        }
        // If first character is '>', check if it's followed by '=' to make ">="
        else if (c == '>') {
            if (n == '=') {
                next(); // Consume the '='
                op = ">="; // Update to greater-than-or-equal operator
            }
        }

        // Return the operator token (either single or compound)
        return new Token(TokenType.OPERATOR, op, startLine, startCol);
    }

    // =================================
    // COMMENT SCANNING
    // =================================

    /**
     * Handle '/' which could be division, single-line comment, or multi-line
     * comment
     */
    private Token scanSlashOrComment() {
        // Save the starting position for error reporting
        int startLine = line, startCol = col;
        next(); // Consume the '/' character
        char n = peek(); // Look at the next character to determine what kind of slash this is

        // If followed by another '/', it's a single-line comment
        if (n == '/') {
            // Single-line comment: skip until newline
            next(); // Consume the second '/'
            // Skip all characters until we reach a newline or end of file
            while (!eof() && peek() != '\n')
                next();
            // If we found a newline, consume it too
            if (!eof() && peek() == '\n')
                next(); // consume newline
            // Recursively get the next token after the comment
            return nextToken();
        }
        // If followed by '*', it's the start of a multi-line comment
        else if (n == '*') {
            // Multi-line comment with nesting support
            next(); // Consume the '*'
            int nestingLevel = 1; // Track nesting depth for nested comments

            // Continue until we find the end of the comment or reach EOF
            while (!eof()) {
                char c = next(); // Get the next character
                // If we find "/*", it's a nested comment, increase nesting level
                if (c == '/' && peek() == '*') {
                    next(); // Consume the '*'
                    nestingLevel++;
                }
                // If we find a '*', it might be the end of a comment
                else if (c == '*') {
                    // Skip multiple consecutive '*' characters
                    while (peek() == '*')
                        next();
                    // If followed by '/', it's the end of a comment
                    if (peek() == '/') {
                        next(); // Consume the '/'
                        nestingLevel--; // Decrease nesting level
                        // If nesting level is 0, we've found the matching end
                        if (nestingLevel == 0)
                            break;
                    }
                }
            }

            // If we reached EOF without closing all comments, it's an error
            if (nestingLevel != 0) {
                return new Token(TokenType.ERROR, "unterminated comment", startLine, startCol);
            }
            // Recursively get the next token after the comment
            return nextToken();
        }
        // If not followed by '/' or '*', it's just a division operator
        else {
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
        next(); // Consume the opening '$' delimiter
        StringBuilder sb = new StringBuilder(); // Build the string content

        // Continue until we reach end of file
        while (!eof()) {
            char c = next(); // Get the next character

            // If we find the closing '$', we have a valid string
            if (c == '$') {
                // Found closing delimiter - return successful string token
                return new Token(TokenType.STRING, sb.toString(), startLine, startCol);
            }

            // Check for forbidden \n escape sequence
            if (c == '\\') {
                if (peek() == 'n') {
                    next(); // Consume the 'n' after the backslash
                    // Error recovery: skip until next '$' or newline
                    while (!eof() && peek() != '$' && peek() != '\n')
                        next();
                    if (!eof() && peek() == '$')
                        next();
                    // Return error for forbidden escape sequence
                    return new Token(TokenType.ERROR, "string contains forbidden \\n", startLine, startCol);
                } else {
                    // Other backslashes are treated as literal characters
                    sb.append(c);
                    continue; // Skip the rest of the loop and get next character
                }
            }

            // Real newline inside string is an error (strings can't span multiple lines)
            if (c == '\n' || c == '\0') {
                // Error recovery: skip until next '$' delimiter
                while (!eof() && peek() != '$') {
                    if (peek() == '\n')
                        break; // Stop at another newline
                    next();
                }
                if (!eof() && peek() == '$')
                    next();
                // Return error for unterminated string
                return new Token(TokenType.ERROR, "unterminated string", startLine, startCol);
            }

            // Add character to string content (including spaces and all other valid chars)
            sb.append(c);
        }

        // If we reach EOF without finding the closing delimiter, it's an error
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
        StringBuilder sb = new StringBuilder(); // Build the word

        // Consume consecutive letters to form the word
        while (!eof() && isLetter(peek())) {
            sb.append(next()); // Add each letter to the word
        }

        // Convert the collected letters to a string
        String word = sb.toString();
        // Check if the word is one of our reserved keywords
        if (KEYWORDS.contains(word)) {
            // It's a valid keyword, return a keyword token
            return new Token(TokenType.KEYWORD, word, startLine, startCol);
        }

        // If not a keyword, it's an error (regular identifiers must start with team
        // prefixes)
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
        // Save current state for potential rollback if this isn't a valid identifier
        int savedPos = i, savedLine = line, savedCol = col;

        // Identifiers must start with '1' (first digit of team prefixes)
        if (peek() != '1')
            return null;

        // Check for valid team prefixes (134, 104, 199)
        if (peek(1) == '3' && peek(2) == '4') {
            next();
            next();
            next(); // Consume "134" prefix
        } else if (peek(1) == '0' && peek(2) == '4') {
            next();
            next();
            next(); // Consume "104" prefix
        } else if (peek(1) == '9' && peek(2) == '9') {
            next();
            next();
            next(); // Consume "199" prefix
        } else {
            return null; // Not a valid team prefix
        }

        // Check if there's a valid identifier body (must start with letter or
        // underscore)
        if (!isLetterOrUnderscore(peek())) {
            // Team prefix found but no valid identifier body
            // Rollback to saved state and let number scanner handle it as INTEGER
            i = savedPos;
            line = savedLine;
            col = savedCol;
            return null;
        }

        // Valid identifier: consume the rest of the body (letters, digits, underscores)
        while (!eof() && isLetterDigitUnderscore(peek())) {
            next(); // Consume each valid character in the identifier body
        }

        // Extract the full identifier text from the saved position to current position
        String lexeme = src.substring(savedPos, i);
        // Return the identifier token
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
        int startPos = i; // Save starting position for extracting the lexeme
        // Flags to track what parts of the number we've seen
        boolean seenInt = false, seenDot = false, seenFrac = false, seenExp = false;

        // Handle numbers starting with decimal point (.5, .25e10)
        if (peek() == '.') {
            seenDot = true; // Mark that we've seen a decimal point
            next(); // Consume the '.'

            // Check if there are digits after the decimal point
            if (isDigit(peek())) {
                // Valid case: .123
                while (isDigit(peek())) {
                    next(); // Consume each digit
                    seenFrac = true; // Mark that we've seen fractional digits
                }
            } else if (peek() == 'e' || peek() == 'E') {
                // Invalid case: .e10 (no fractional part before exponent)
                next(); // Consume the 'e'/'E'
                if (peek() == '+' || peek() == '-')
                    next(); // Consume optional sign
                while (isDigit(peek()))
                    next(); // Consume any digits after
                String badToken = src.substring(startPos, i);
                return new Token(TokenType.ERROR, "bad exponent: " + badToken, startLine, startCol);
            } else {
                // Just a lone dot - treat as error
                i = startPos; // Reset position
                char dot = next(); // Consume just the dot
                return new Token(TokenType.ERROR, String.valueOf(dot), startLine, startCol);
            }
        } else {
            // Handle numbers starting with digits
            while (isDigit(peek())) {
                next(); // Consume each digit
                seenInt = true; // Mark that we've seen integer part
            }

            // Check for decimal point after integer part
            if (peek() == '.') {
                seenDot = true; // Mark that we've seen a decimal point
                next(); // Consume the '.'

                // Check for malformed case with two dots (12..3)
                if (peek() == '.') {
                    next(); // Consume the second dot
                    while (isDigit(peek()))
                        next(); // Consume any digits after
                    String badToken = src.substring(startPos, i);
                    return new Token(TokenType.ERROR, "malformed number: " + badToken, startLine, startCol);
                }

                // Consume fractional digits after decimal point
                while (isDigit(peek())) {
                    next(); // Consume each digit
                    seenFrac = true; // Mark that we've seen fractional digits
                }
            }
        }

        // Handle scientific notation (e or E)
        if (peek() == 'e' || peek() == 'E') {
            // Check for malformed float before exponent (e.g., 10.e5 with no fractional
            // part)
            if (seenDot && !seenFrac) {
                next(); // Consume the 'e'/'E'
                if (peek() == '+' || peek() == '-')
                    next(); // Consume optional sign
                while (isDigit(peek()))
                    next(); // Consume any digits after
                String badToken = src.substring(startPos, i);
                return new Token(TokenType.ERROR, "malformed float: " + badToken, startLine, startCol);
            }

            seenExp = true; // Mark that we've seen an exponent
            next(); // Consume the 'e'/'E'

            // Optional sign for exponent
            if (peek() == '+' || peek() == '-')
                next();

            // Must have digits after e/E
            if (!isDigit(peek())) {
                if (isLetter(peek()))
                    next(); // Consume the invalid character
                String badToken = src.substring(startPos, Math.max(i, startPos + 1));
                return new Token(TokenType.ERROR, "bad exponent: " + badToken, startLine, startCol);
            }

            // Consume all digits in the exponent
            while (isDigit(peek()))
                next();
        }

        // Extract the full number lexeme from start to current position
        String lexeme = src.substring(startPos, i);

        // Final validation - if we saw a dot but no fractional digits, it's malformed
        if (seenDot && !seenFrac) {
            return new Token(TokenType.ERROR, "malformed float: " + lexeme, startLine, startCol);
        }

        // Determine token type based on what parts we saw
        if (seenDot || seenExp) {
            // If we saw a decimal point or exponent, it's a floating-point NUMBER
            return new Token(TokenType.NUMBER, lexeme, startLine, startCol);
        }
        if (seenInt) {
            // If we only saw integer digits, it's an INTEGER
            return new Token(TokenType.INTEGER, lexeme, startLine, startCol);
        }

        // Fallback error case - shouldn't normally reach here
        return new Token(TokenType.ERROR, src.substring(startPos, Math.min(startPos + 1, src.length())), startLine,
                startCol);
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
        // Create a Scanner to read from standard input (keyboard)
        Scanner scanner = new Scanner(System.in);
        // Prompt user for input
        System.out.println("Enter source code (type 'END' on a separate line to finish):");
        // Use StringBuilder to efficiently build the input string
        StringBuilder content = new StringBuilder();

        // Loop indefinitely until we encounter the "END" marker
        while (true) {
            // Check if there's another line of input
            if (!scanner.hasNextLine())
                break;
            // Read the next line
            String line = scanner.nextLine();
            // If the line is "END", stop reading
            if (line.equals("END"))
                break;
            // Add the line to our content with a newline character
            content.append(line).append('\n');
        }

        // Return the complete input as a string
        return content.toString();
    }

    /**
     * Read input from a file
     * If file reading fails, falls back to standard input
     */
    private static String readFromFile(String filePath) {
        try {
            // Use Java NIO's Files class to read the entire file content as a string
            return Files.readString(Path.of(filePath));
        } catch (IOException e) {
            // If there's an error reading the file, print an error message
            System.err.println("Error reading file '" + filePath + "': " + e.getMessage());
            // Return null to indicate failure (caller will decide what to do)
            return null; // Return null instead of falling back automatically
        }
    }

    /**
     * Print usage instructions for the program
     */
    private static void printUsage() {
        // Print a header for the usage instructions
        System.out.println("Usage:");
        // Print the different ways to run the program
        System.out.println("  java Main                    # Interactive mode - choose input type");
        System.out.println("  java Main <filename>         # Direct file input");
        System.out.println("  java Main -f <filename>      # Direct file input (explicit flag)");
        System.out.println("  java Main -c                 # Direct console input");
    }

    /**
     * Interactive method to let user choose input type
     */
    private static String chooseInputSource() {
        // Create a Scanner to read user input
        Scanner scanner = new Scanner(System.in);

        // Loop indefinitely until we get a valid choice and successful input
        while (true) {
            // Display menu options
            System.out.println("Choose input source:");
            System.out.println("1. Console input (type code directly)");
            System.out.println("2. File input (read from file)");
            System.out.print("Enter your choice (1 or 2): ");

            // Check if there's any input
            if (!scanner.hasNextLine()) {
                System.err.println("No input received. Exiting.");
                System.exit(1); // Exit with error code
            }

            // Get the user's choice and trim whitespace
            String choice = scanner.nextLine().trim();

            // Handle console input choice
            if (choice.equals("1")) {
                // Console input chosen
                return readFromStdin();
            }
            // Handle file input choice
            else if (choice.equals("2")) {
                // File input chosen
                System.out.print("Enter file path: ");
                // Check if there's a file path
                if (!scanner.hasNextLine()) {
                    System.err.println("No file path provided. Exiting.");
                    System.exit(1); // Exit with error code
                }
                // Get the file path and trim whitespace
                String filePath = scanner.nextLine().trim();

                // Try to read the file
                String content = readFromFile(filePath);
                // If file reading failed
                if (content == null) {
                    // Ask user if they want to try again or switch to console input
                    System.out.println("Would you like to try again or switch to console input?");
                    System.out.println("1. Try another file");
                    System.out.println("2. Switch to console input");
                    System.out.print("Choice (1 or 2): ");

                    // Check if there's a response
                    if (!scanner.hasNextLine()) {
                        System.err.println("No choice provided. Exiting.");
                        System.exit(1); // Exit with error code
                    }

                    // Get the retry choice and trim whitespace
                    String retry = scanner.nextLine().trim();
                    if (retry.equals("2")) {
                        // User chose to switch to console input
                        return readFromStdin();
                    }
                    // Continue loop to try another file
                    continue;
                }
                // File was read successfully, return its content
                return content;
            } else {
                // Invalid choice, prompt again
                System.out.println("Invalid choice. Please enter 1 or 2.");
            }
        }
    }

    /**
     * Main entry point
     * Handles command-line arguments, reads input, tokenizes, and displays results
     */
    public static void main(String[] args) {
        String input; // Variable to store the input source code

        // Parse command-line arguments to determine input source
        if (args.length == 0) {
            // No arguments: interactive mode - let user choose input type
            input = chooseInputSource();
        } else if (args.length == 1) {
            // One argument: could be help flag, console flag, or filename
            if (args[0].equals("-h") || args[0].equals("--help")) {
                // User requested help information
                printUsage();
                return; // Exit after showing help
            } else if (args[0].equals("-c")) {
                // Direct console input mode
                input = readFromStdin();
            } else {
                // Single argument: treat as filename
                input = readFromFile(args[0]);
                if (input == null) {
                    System.err.println("Failed to read file. Exiting.");
                    return; // Exit if file couldn't be read
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("-f")) {
            // Two arguments with -f flag: read from specified file
            input = readFromFile(args[1]);
            if (input == null) {
                System.err.println("Failed to read file. Exiting.");
                return; // Exit if file couldn't be read
            }
        } else {
            // Invalid arguments - show usage and exit
            System.err.println("Invalid arguments.");
            printUsage();
            return;
        }

        // Create lexer with the input source code
        Lexer lexer = new Lexer(input);
        // Create a list to store all tokens
        List<Token> tokens = new ArrayList<>();

        // Extract all tokens until we reach EOF
        while (true) {
            // Get the next token from the lexer
            Token token = lexer.nextToken();
            // Add the token to our list
            tokens.add(token);
            // If it's an EOF token, we're done
            if (token.type == TokenType.EOF)
                break;
        }

        // Display the tokenization results
        System.out.println("=== TOKENS ===");
        // Print each token on a separate line
        for (Token token : tokens) {
            System.out.println(token);
        }
    }
}
