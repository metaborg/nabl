package mb.statix.random.util;

import java.io.PrintStream;

public class StreamProgressPrinter implements IProgressPrinter {

    private final PrintStream out;
    private final int lineWidth;

    private int count = 0;

    public StreamProgressPrinter(PrintStream out, int lineWidth) {
        this.out = out;
        this.lineWidth = lineWidth;
    }

    @Override public void step(char c) {
        if(c == '\n' || (++count % lineWidth) == 0) {
            count = 0;
            out.println();
        } else {
            out.print(c);
        }
    }

    @Override public void done() {
        if((count % lineWidth) != 0) {
            out.println();
        }
    }

}
