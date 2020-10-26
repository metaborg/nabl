package mb.statix.benchmarks;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import mb.nabl2.terms.ITerm;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public class CommonsCSVBenchmark {

    private static final ILogger logger = LoggerUtils.logger(CommonsCSVBenchmark.class);

    public static void main(String[] args) throws Exception, InterruptedException {
        Spec spec = BenchmarkUtil.loadSpecFromATerm("metaborg-java.mergedspec.aterm");
        java.util.Map<String, ITerm> files = BenchmarkUtil.loadFilesFromATerm("commons-csv.files.aterm");
        java.util.Map<String, Rule> units = BenchmarkUtil.makeFileUnits(files, "statics!projectOk", "statics!fileOk");
        int parallelism = Runtime.getRuntime().availableProcessors();
        final long t0 = System.currentTimeMillis();
        BenchmarkUtil.runConcurrent(spec, units, parallelism);
        final long dt = System.currentTimeMillis() - t0;
        logger.info("Finished after {} s.", dt / 1_000d);
    }

    @State(Scope.Thread)
    public static class Input {

        Spec spec;
        java.util.Map<String, Rule> units;
        int parallelism = Runtime.getRuntime().availableProcessors();

        public Input() {
            try {
                this.spec = BenchmarkUtil.loadSpecFromATerm("metaborg-java.mergedspec.aterm");
                java.util.Map<String, ITerm> files = BenchmarkUtil.loadFilesFromATerm("commons-csv.files.aterm");
                this.units = BenchmarkUtil.makeFileUnits(files, "statics!projectOk", "statics!fileOk");
            } catch(Throwable t) {
                throw new RuntimeException(t);
            }
        }

    }

    @Benchmark @BenchmarkMode(Mode.All) public Object run(Input input) throws Exception, InterruptedException {
        return BenchmarkUtil.runConcurrent(input.spec, input.units, input.parallelism);
    }

}