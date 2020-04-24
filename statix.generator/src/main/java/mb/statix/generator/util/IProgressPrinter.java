package mb.statix.generator.util;

public interface IProgressPrinter {

    void step(char c);

    void done();

}