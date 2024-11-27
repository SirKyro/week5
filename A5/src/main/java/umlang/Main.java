package umlang;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import immutable.List;
import immutable.Some;
import syntax.TermReader;
import syntax.TermSyntaxError;
import umlang.ast.ParseError;
import umlang.ast.Parser;
import umlang.errors.InterpretationException;

/** Main entry point for umlang. */
public class Main {
    public static void main(String[] args) throws IOException, ParseError, InterpretationException {
        var interpreter = Interpreter.newDefault();

        if (args.length > 0) {
            for (var filename : args) {
                interpreter.loadProgram(filename);
            }
        }

        readEvalPrintLoop(new InputStreamReader(System.in), interpreter);
    }

    /**
     * Repeatedly uses `TermReader` to accept S-expressions from `input`,
     * parse them, interpret them using `interpreter`, and print the results.
     */
    public static void readEvalPrintLoop(Reader input, Interpreter interpreter) {
        var reader = new TermReader(input);
        while (true) {
            System.out.print("> ");
            System.out.flush();
            try {
                var term = reader.next();
                if (term == null) break;
                var ast = Parser.parseToplevel(term);
                if (interpreter.evaluateProgram(List.of(ast)) instanceof Some(var result)) {
                    System.out.println(result.toDisplayableString());
                }
            } catch (TermSyntaxError e) {
                System.err.println(e);
                reader.reset();
            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }
}
