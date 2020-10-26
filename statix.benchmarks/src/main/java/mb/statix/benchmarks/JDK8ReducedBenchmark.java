package mb.statix.benchmarks;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITerm;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public class JDK8ReducedBenchmark {

    private static final ILogger logger = LoggerUtils.logger(JDK8ReducedBenchmark.class);

    public static void main(String[] args) throws Exception, InterruptedException {
        Spec spec = BenchmarkUtil.loadSpecFromATerm("metaborg-java.mergedspec.aterm");
        java.util.Map<String, ITerm> files = BenchmarkUtil.loadFilesFromATerm("jdk8_reduced.files.aterm");
        java.util.Map<String, Rule> units = BenchmarkUtil.makeFileUnits(files, "statics!projectOk", "statics!fileOk");
        int parallelism = Runtime.getRuntime().availableProcessors();
        final long t0 = System.currentTimeMillis();
        BenchmarkUtil.runConcurrent(spec, units, parallelism);
        final long dt = System.currentTimeMillis() - t0;
        logger.info("Finished after {} s.", dt / 1_000d);
    }

}