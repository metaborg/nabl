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

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Threads(1)
@State(Scope.Thread)
public class JDK8Benchmark extends JavaBenchmark {

    private static final ILogger logger = LoggerUtils.logger(JDK8Benchmark.class);

    @Param({ "1", "2", "4", "8" }) public int parallelism;

    @Override protected String name() {
        return "jdk8";
    }

    @Setup(Level.Trial) @Override public void doSetup() {
        super.doSetup();
    }

    @Benchmark public Object run() throws Exception, InterruptedException {
        return super.doRun(parallelism);
    }

    public static void main(String[] args) throws Exception, InterruptedException {
        JavaBenchmark benchmark = new JDK8Benchmark();
        benchmark.doSetup();
        final long t0 = System.currentTimeMillis();
        benchmark.doRun(Runtime.getRuntime().availableProcessors());
        final long dt = System.currentTimeMillis() - t0;
        logger.info("Finished after {} s.", dt / 1_000d);
    }

}