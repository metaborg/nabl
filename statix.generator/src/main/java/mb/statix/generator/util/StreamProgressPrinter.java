package mb.statix.generator.util;

import java.io.PrintStream;
import java.util.Collections;

import org.metaborg.util.functions.Action1;

public class StreamProgressPrinter implements IProgressPrinter {

    private final PrintStream out;
    private final int lineWidth;
    private final Action1<PrintStream> eol;

    private int count = 0;

    public StreamProgressPrinter(PrintStream out, int lineWidth) {
        this(out, lineWidth, out::println);
    }

    public StreamProgressPrinter(PrintStream out, int lineWidth, Action1<PrintStream> eol) {
        this.out = out;
        this.lineWidth = lineWidth;
        this.eol = eol;
    }

    @Override public void step(char c) {
        if(c == '\n' || (++count % lineWidth) == 0) {
            out.print(repeatString(" ", lineWidth - count));
            eol.apply(out);
            count = 0;
        } else {
            out.print(c);
        }
    }

    @Override public void done() {
        if((count % lineWidth) != 0) {
            out.print(repeatString(" ", lineWidth - count - 1));
            eol.apply(out);
        }
    }

    private static String repeatString(String s, int n) {
        return String.join("", Collections.nCopies(n, s));
    }

}
