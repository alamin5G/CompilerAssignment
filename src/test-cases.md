### How to use

- Paste each “Input” into the program, terminate with END, and compare the printed tokens to the “Expected” column.[^8]
- Token types: IDENTIFIER (team-prefix IDs), KEYWORD, INTEGER (pure digits), NUMBER (decimal and/or exponent), STRING (single-line $…$), OPERATOR, ERROR, EOF.[^8]


### Identifiers — valid

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| I1 | 134Alamin | IDENTIFIER('134Alamin') | Team prefix + letters |
| I2 | 134_Alqmin | IDENTIFIER('134_Alqmin') | Underscore allowed in body |
| I3 | 104Ritu | IDENTIFIER('104Ritu') | Alternate team prefix |
| I4 | 199ABC123 | IDENTIFIER('199ABC123') | Letters and digits in body |
| I5 | 104_user1 | IDENTIFIER('104_user1') | Body may start with underscore |

### Identifiers — invalid

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| I6 | 134 | ERROR('unknown word:') or ERROR (no body) | Prefix without body is invalid by spec |
| I7 | _abc | ERROR('_') | Must not start without team prefix |
| I8 | main | ERROR('unknown word: main') | Only listed keywords are reserved; other words error |
| I9 | 13A | ERROR('1') then ERROR('3')… | Not one of 134/104/199 prefixes |
| I10 | 1999x | ERROR('1') or NUMBER/ERROR split | Not a legal team prefix path |

### Keywords

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| K1 | if | KEYWORD('if') | Reserved word |
| K2 | else while return func | KEYWORD('else'), KEYWORD('while'), KEYWORD('return'), KEYWORD('func') | All reserved |
| K3 | 134func | IDENTIFIER('134func') | Prefix forces identifier, not keyword |

### Numbers — valid (INTEGER vs NUMBER)

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| N1 | 0 | INTEGER('0') | Pure digits → INTEGER |
| N2 | 7 | INTEGER('7') | Pure digits → INTEGER |
| N3 | 007 | INTEGER('007') | Leading zeros allowed |
| N4 | 10.5 | NUMBER('10.5') | Decimal with digits on both sides |
| N5 | .5 | NUMBER('.5') | Leading-dot style allowed |
| N6 | 5. | ERROR('malformed float: 5.') | Must have fraction digits after dot |
| N7 | 10E15 | NUMBER('10E15') | Exponent on integer |
| N8 | 10e-10 | NUMBER('10e-10') | Exponent with sign |
| N9 | 3.14159e+0 | NUMBER('3.14159e+0') | Decimal with exponent |
| N10 | .25E2 | NUMBER('.25E2') | Leading-dot with exponent |

### Numbers — invalid (malformed)

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| N11 | .e5 | ERROR('bad exponent: .e5') | Dot not followed by digits |
| N12 | 10.e5 | ERROR('malformed float: 10.e5') | No digits after dot before exponent |
| N13 | 10. | ERROR('malformed float: 10.') | Trailing dot without fraction |
| N14 | 12..3 | ERROR('malformed number: 12..3') | Double-dot |
| N15 | 1e+ | ERROR('bad exponent: 1e+') | Exponent missing digits |
| N16 | e10 | ERROR('unknown word: e') | Must start with digit or dot |

### Strings — valid

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| S1 | $ok$ | STRING('ok') | Single-line string delimited by \$ |
| S2 | \$\$ | STRING('') | Empty string allowed |
| S3 | $HELLO WORLD$ | STRING('HELLO WORLD') | Spaces allowed inside |

### Strings — invalid and recovery

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| S4 | \$unterminated | ERROR('unterminated string') | EOF before closing \$ |
| S5 | $line1\nline2$ (real newline) | ERROR('unterminated string') | Newline not allowed inside |
| S6 | $hello\n$ (literal backslash-n) | ERROR('string contains forbidden \n') | Literal “\n” forbidden per decision; closes by recovery to next \$ |
| S7 | $a\\b$ | STRING('a\\b') | Backslash by itself is ordinary char unless it forms “\n” |

### Operators — valid

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| O1 | + - * / | OPERATOR('+'), OPERATOR('-'), OPERATOR('*'), OPERATOR('/') | Single-char ops |
| O2 | = == != <= >= < > | OPERATOR('='), OPERATOR('=='), OPERATOR('!='), OPERATOR('<='), OPERATOR('>='), OPERATOR('<'), OPERATOR('>') | All listed |
| O3 | <> | OPERATOR('<>') | Not-equal (extra) |
| O4 | ! | ERROR('!') | Bare ‘!’ invalid |

### Comments — single-line (skip)

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| C1 | // hello | (no token; next token after newline) | Skipped entirely |
| C2 | if // hi⏎ return | KEYWORD('if'), KEYWORD('return') | Comment removed; tokens continue |
| C3 | // only⏎ | (no token; then EOF) | End at newline/EOF |

### Comments — multi-line valid (nested, stars, backslashes)

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| M1 | /* comment */ | (no token; continue) | Basic block |
| M2 | /* a /* b */ c */ | (no token; nested handled) | Counter++/-- nesting |
| M3 | /* comment\n \* hello\n \* hi */ | (no token) | Body lines with leading “*” |
| M4 | /* hello\n\hi\n\how are you **/ | (no token) | Multiple ‘*’ before ‘/’ close |
| M5 | /* this is \comment */ | (no token) | Backslash treated as ordinary char |

### Comments — invalid

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| M6 | /* unterminated | ERROR('unterminated comment') | EOF before closing */ |
| M7 | / * notcomment | OPERATOR('/') then ERROR('*') or word errors | Not “/*” exactly; per DFA |

### Slash disambiguation

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| SD1 | /* a */ / + .5 | OPERATOR('/') after comment, OPERATOR('+'), NUMBER('.5') | Slash returns to operator after block |
| SD2 | /x | OPERATOR('/') then ERROR('x') or next token | Not a comment; plain operator |

### Mixed sequences

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| MX1 | func 134Alamin = 10.5 + .5 | KEYWORD('func'), IDENTIFIER('134Alamin'), OPERATOR('='), NUMBER('10.5'), OPERATOR('+'), NUMBER('.5') | Typical statement-like mix |
| MX2 | if 134_Alqmin <> 104Ritu | KEYWORD('if'), IDENTIFIER('134_Alqmin'), OPERATOR('<>'), IDENTIFIER('104Ritu') | Decision with <> |
| MX3 | $ok$ 10E15 10e-10 // done | STRING('ok'), NUMBER('10E15'), NUMBER('10e-10') | Comment skipped at end |
| MX4 | func $bad\n$ | KEYWORD('func'), ERROR('string contains forbidden \n') | Literal “\n” inside string |
| MX5 | 104_users /* note */ return | IDENTIFIER('104_users'), KEYWORD('return') | Comment removed |

### Boundary/whitespace cases

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| B1 | ␠␠134A␠ | IDENTIFIER('134A') | Leading/trailing spaces ignored |
| B2 | \t10.5\t | NUMBER('10.5') | Tabs ignored |
| B3 | \n// cmt\nif | KEYWORD('if') | Newlines OK; comment dropped |

### Unknown/unsupported symbols (since delimiters not specified)

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| U1 | ; | ERROR(';') | Delimiters not in spec → error |
| U2 | (134A) | ERROR('('), IDENTIFIER('134A'), ERROR(')') | Parentheses treated as errors per decision |
| U3 | { } [ ] , | ERROR('{'), ERROR('}'), ERROR('['), ERROR(']'), ERROR(',') | All not in token set |

### Identifier prefix edge cases

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| P1 | 199 | ERROR or no-body error | Prefix without body invalid |
| P2 | 1049a | ERROR('1')… | Not a valid team prefix path |
| P3 | 1349a | ERROR('1')… | 134 must be followed by letter/_ |

### Exponent edge cases

| \# | Input | Expected | Notes |
| :-- | :-- | :-- | :-- |
| E1 | 1e | ERROR('bad exponent: 1e') | Missing digits |
| E2 | 1e+ | ERROR('bad exponent: 1e+') | Missing digits after sign |
| E3 | .5e+7 | NUMBER('.5e+7') | Valid dot-leading with exponent |
| E4 | 5.e+7 | ERROR('malformed float: 5.e+7') | Missing fraction before exponent |

If any policy change is preferred (e.g., ignoring delimiters instead of error), the “Unknown/unsupported symbols” expectations can be switched from ERROR to “ignored,” and that should be documented in the report’s design decisions for full marks.
<span style="display:none">[^1][^2][^3][^4][^5][^6][^7]</span>

<div style="text-align: center">⁂</div>

[^1]: https://amirkamil.github.io/project-scheme-parser/

[^2]: https://www.geeksforgeeks.org/c/c-lexical-analyser-lexer/

[^3]: https://web.eecs.umich.edu/~weimerw/2015-4610/pa2/pa2.html

[^4]: https://www.cs.ucr.edu/~dtan004/proj1/phase1_lexer.html

[^5]: https://serhii.io/posts/writing-your-own-lexer-with-simple-steps

[^6]: http://staff.ustc.edu.cn/~bjhua/courses/compiler/2014/labs/lab1/index.html

[^7]: https://www.reddit.com/r/Compilers/comments/z6qe98/best_approach_for_writing_a_lexer/

[^8]: compiler-assignment.pdf

