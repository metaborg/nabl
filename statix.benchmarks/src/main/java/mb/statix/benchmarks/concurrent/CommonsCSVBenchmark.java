package mb.statix.benchmarks.concurrent;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITerm;
import mb.statix.benchmarks.InputLoader;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public class CommonsCSVBenchmark {

    private static final ILogger logger = LoggerUtils.logger(CommonsCSVBenchmark.class);

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        Spec spec = InputLoader.loadSpecFromATerm("metaborg-java.mergedspec.aterm");
        java.util.Map<String, ITerm> files = InputLoader.loadFilesFromATerm("commons-csv.files.aterm");
        java.util.Map<String, Rule> units =
                ConcurrentRunner.makeFileUnits(files, "statics!projectOk", "statics!fileOk");
        final long t0 = System.currentTimeMillis();
        ConcurrentRunner.run(spec, units);
        final long dt = System.currentTimeMillis() - t0;
        logger.info("Finished after {} s.", dt / 1_000d);
    }

}