// LexerDemo.java (final polished)
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

enum TokenType { IDENTIFIER, KEYWORD, INTEGER, NUMBER, STRING, OPERATOR, ERROR, EOF }


class Token {
    final TokenType type;
    final String lexeme;
    final int line, col;
    Token(TokenType t, String x, int line, int col) {
        this.type = t; this.lexeme = x; this.line = line; this.col = col;
    }
    public String toString() { return type + "('" + lexeme + "')@" + line + ":" + col; }
}

class Lexer {
    private final String src;
    private int i = 0, line = 1, col = 1;

    private static final Set<String> KEYWORDS = Set.of("if","else","while","return","func");

    Lexer(String input) { this.src = input; }

    private boolean eof() { return i >= src.length(); }
    private char peek() { return eof() ? '\0' : src.charAt(i); }
    private char peek(int k) { int j = i + k; return j >= src.length() ? '\0' : src.charAt(j); }
    private char next() {
        char c = peek();
        if (c == '\0') return c;
        i++;
        if (c == '\n') { line++; col = 1; } else { col++; }
        return c;
    }
    private boolean isLetter(char c) { return (c>='A'&&c<='Z')||(c>='a'&&c<='z'); }
    private boolean isDigit(char c) { return c>='0'&&c<='9'; }
    private boolean isLetterOrUnderscore(char c){ return isLetter(c) || c=='_'; }
    private boolean isLetterDigitUnderscore(char c){ return isLetter(c)||isDigit(c)||c=='_'; }

    private void skipWhitespace() {
        while(!eof()){
            char c = peek();
            if (c==' '||c=='\t'||c=='\r'||c=='\n') { next(); }
            else break;
        }
    }

    public Token nextToken() {
        skipWhitespace();
        if (eof()) return new Token(TokenType.EOF, "<EOF>", line, col);

        char c = peek();
        int startLine = line, startCol = col;

        if (c == '/') return scanSlashOrComment();
        if (c == '$') return scanString(startLine, startCol);
        if (c == '1') {
            Token id = tryScanIdentifier(startLine, startCol);
            if (id != null) return id;
            return scanNumber(startLine, startCol);
        }
        if (isLetter(c)) return scanKeywordOrError(startLine, startCol);
        if (isDigit(c) || c == '.') return scanNumber(startLine, startCol);
        if ("+-=*<>!".indexOf(c) >= 0) return scanOperator(startLine, startCol);

        next();
        return new Token(TokenType.ERROR, String.valueOf(c), startLine, startCol);
    }

    private Token scanOperator(int startLine, int startCol) {
        char c = next();
        char n = peek();
        String op = String.valueOf(c);
        if (c == '=') {
            if (n == '=') { next(); op = "=="; }
        } else if (c == '!') {
            if (n == '=') { next(); op = "!="; }
            else return new Token(TokenType.ERROR, "!", startLine, startCol);
        } else if (c == '<') {
            if (n == '=') { next(); op = "<="; }
            else if (n == '>') { next(); op = "<>"; }
        } else if (c == '>') {
            if (n == '=') { next(); op = ">="; }
        }
        return new Token(TokenType.OPERATOR, op, startLine, startCol);
    }

    private Token scanSlashOrComment() {
        int startLine = line, startCol = col;
        next(); // consume '/'
        char n = peek();
        if (n == '/') {
            next(); // second '/'
            while(!eof() && peek()!='\n') next();
            if (!eof() && peek()=='\n') next();
            return nextToken();
        } else if (n == '*') {
            next(); // consume '*'
            int counter = 1;
            while(!eof()) {
                char c = next();
                if (c == '/' && peek()=='*') {
                    next(); // '*'
                    counter++;
                } else if (c == '*') {
                    while (peek()=='*') next();
                    if (peek()=='/') {
                        next(); // '/'
                        counter--;
                        if (counter == 0) break;
                    }
                }
            }
            if (counter != 0) {
                return new Token(TokenType.ERROR, "unterminated comment", startLine, startCol);
            }
            return nextToken();
        } else {
            return new Token(TokenType.OPERATOR, "/", startLine, startCol);
        }
    }


    private Token scanString(int startLine, int startCol) {
        next(); // consume opening $
        StringBuilder sb = new StringBuilder();
        while (!eof()) {
            char c = next();
            if (c == '$') {
                return new Token(TokenType.STRING, sb.toString(), startLine, startCol);
            }
            // Treat literal backslash-n as an error, per conflict spec
            if (c == '\\') {
                if (peek() == 'n') {
                    next(); // consume 'n'
                    // Recovery: skip until next '$' or newline, consume '$' if found
                    while (!eof() && peek() != '$' && peek() != '\n') next();
                    if (!eof() && peek() == '$') next();
                    return new Token(TokenType.ERROR, "string contains forbidden \\n", startLine, startCol);
                } else {
                    // Other backslashes are treated as ordinary characters (no escapes in spec)
                    sb.append(c);
                    continue;
                }
            }
            // Real newline inside string is also an error
            if (c == '\n' || c == '\0') {
                // Recovery: skip until next '$', consume it if present
                while (!eof() && peek() != '$') {
                    // stop if another newline encountered
                    if (peek() == '\n') break;
                    next();
                }
                if (!eof() && peek() == '$') next();
                return new Token(TokenType.ERROR, "unterminated string", startLine, startCol);
            }
            sb.append(c);
        }
        return new Token(TokenType.ERROR, "unterminated string", startLine, startCol);
    }


    private Token scanKeywordOrError(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        while (!eof() && isLetter(peek())) sb.append(next());
        String w = sb.toString();
        if (KEYWORDS.contains(w)) return new Token(TokenType.KEYWORD, w, startLine, startCol);
        return new Token(TokenType.ERROR, "unknown word: "+w, startLine, startCol);
    }

    private Token tryScanIdentifier(int startLine, int startCol) {
        int si = i, sl = line, sc = col;
        if (peek()!='1') return null;

        // Check for team prefixes and consume them
        if (peek(1)=='3' && peek(2)=='4') {
            next(); next(); next(); // consume "134"
        } else if (peek(1)=='0' && peek(2)=='4') {
            next(); next(); next(); // consume "104"
        } else if (peek(1)=='9' && peek(2)=='9') {
            next(); next(); next(); // consume "199"
        } else {
            return null;
        }

        if (!isLetterOrUnderscore(peek())) {
            // Team prefix found but no valid identifier body - rollback and let number scanner handle it
            i = si; line = sl; col = sc;
            return null;
        }

        // Valid identifier: consume the rest of the body
        while (!eof() && isLetterDigitUnderscore(peek())) next();
        String lex = src.substring(si, i);
        return new Token(TokenType.IDENTIFIER, lex, startLine, startCol);
    }

    private Token scanNumber(int startLine, int startCol) {
        int si = i;
        boolean seenInt = false, seenDot = false, seenFrac = false, seenExp = false;

        if (peek()=='.') {
            seenDot = true;
            next(); // '.'
            if (isDigit(peek())) {
                while (isDigit(peek())) { next(); seenFrac = true; }
            } else if (peek()=='e' || peek()=='E') {
                // ".e..." -> bad exponent
                next(); // e/E
                if (peek()=='+' || peek()=='-') next();
                while (isDigit(peek())) next();
                String bad = src.substring(si, i);
                return new Token(TokenType.ERROR, "bad exponent: " + bad, startLine, startCol);
            } else {
                i = si; char dot = next();
                return new Token(TokenType.ERROR, String.valueOf(dot), startLine, startCol);
            }
        } else {
            while (isDigit(peek())) { next(); seenInt = true; }
            if (peek()=='.') {
                seenDot = true;
                next();
                if (peek()=='.') { // 12..3
                    next();
                    while (isDigit(peek())) next();
                    String bad = src.substring(si, i);
                    return new Token(TokenType.ERROR, "malformed number: " + bad, startLine, startCol);
                }
                while (isDigit(peek())) { next(); seenFrac = true; }
            }
        }

        // Exponent (allowed on integer or decimal)
        if (peek()=='e' || peek()=='E') {
            // If a dot appeared but NO fractional digits, it's invalid (e.g., 10.e5)
            if (seenDot && !seenFrac) {
                // consume a helpful span to report
                next();
                if (peek()=='+' || peek()=='-') next();
                while (isDigit(peek())) next();
                String bad = src.substring(si, i);
                return new Token(TokenType.ERROR, "malformed float: " + bad, startLine, startCol);
            }
            seenExp = true;
            next(); // e/E
            if (peek()=='+' || peek()=='-') next();
            if (!isDigit(peek())) {
                if (isLetter(peek())) next();
                String bad = src.substring(si, Math.max(i, si+1));
                return new Token(TokenType.ERROR, "bad exponent: " + bad, startLine, startCol);
            }
            while (isDigit(peek())) next();
        }

        String lex = src.substring(si, i);
        if (seenDot && !seenFrac) {
            return new Token(TokenType.ERROR, "malformed float: " + lex, startLine, startCol);
        }
        if (seenDot || seenExp) return new Token(TokenType.NUMBER, lex, startLine, startCol);
        if (seenInt) return new Token(TokenType.INTEGER, lex, startLine, startCol);
        return new Token(TokenType.ERROR, src.substring(si, Math.min(si+1, src.length())), startLine, startCol);
    }
}

public class Main {
    private static String readFromStdin() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter input (type a single line 'END' to finish):");
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (!sc.hasNextLine()) break;
            String line = sc.nextLine();
            if (line.equals("END")) break;
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
    private static String readFromFile(String path) {
        try { return Files.readString(Path.of(path)); }
        catch (IOException e) { System.err.println("Failed to read file: " + e.getMessage()); return ""; }
    }
    public static void main(String[] args) {
        String input = (args.length==2 && args[0].equalsIgnoreCase("-f")) ? readFromFile(args[1]) : readFromStdin();
        Lexer lx = new Lexer(input);
        List<Token> toks = new ArrayList<>();
        while (true) {
            Token t = lx.nextToken();
            toks.add(t);
            if (t.type == TokenType.EOF) break;
        }
        System.out.println("=== TOKENS ===");
        for (Token t : toks) System.out.println(t);
    }
}
