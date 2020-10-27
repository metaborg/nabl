package mb.statix.benchmarks;

import java.util.concurrent.TimeUnit;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

import mb.nabl2.terms.ITerm;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Threads(1)
@State(Scope.Thread)
public class JDK8Benchmark {

    private static final ILogger logger = LoggerUtils.logger(JDK8Benchmark.class);

    @Param({ "1", "2", "4", "8" }) public int parallelism;

    private Spec spec;
    private java.util.Map<String, Rule> units;

    @Setup(Level.Trial) public void doSetup() {
        try {
            this.spec = BenchmarkUtil.loadSpecFromATerm("metaborg-java.mergedspec.aterm");
            java.util.Map<String, ITerm> files = BenchmarkUtil.loadFilesFromATerm("jd8_reduced.files.aterm");
            this.units = BenchmarkUtil.makeFileUnits(files, "statics!projectOk", "statics!fileOk");
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Benchmark public Object run() throws Exception, InterruptedException {
        return BenchmarkUtil.runConcurrent(spec, units, parallelism);
    }

    public static void main(String[] args) throws Exception, InterruptedException {
        Spec spec = BenchmarkUtil.loadSpecFromATerm("metaborg-java.mergedspec.aterm");
        java.util.Map<String, ITerm> files = BenchmarkUtil.loadFilesFromATerm("jdk8.files.aterm");
        java.util.Map<String, Rule> units = BenchmarkUtil.makeFileUnits(files, "statics!projectOk", "statics!fileOk");
        int parallelism = Runtime.getRuntime().availableProcessors();
        final long t0 = System.currentTimeMillis();
        BenchmarkUtil.runConcurrent(spec, units, parallelism);
        final long dt = System.currentTimeMillis() - t0;
        logger.info("Finished after {} s.", dt / 1_000d);
    }

}