package mb.statix.benchmarks;

import mb.nabl2.terms.ITerm;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public abstract class JavaBenchmark {

    private Spec spec;
    private java.util.Map<String, Rule> units;

    protected void doSetup() {
        try {
            this.spec = BenchmarkUtil.loadSpecFromATerm("metaborg-java.mergedspec.aterm");
            java.util.Map<String, ITerm> files = BenchmarkUtil.loadFilesFromATerm(name() + ".files.aterm");
            this.units = BenchmarkUtil.makeFileUnits(files, "statics!projectOk", "statics!fileOk");
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    protected Object doRun(int parallelism) throws Exception, InterruptedException {
        return BenchmarkUtil.runConcurrent(spec, units, parallelism);
    }

    protected abstract String name();

}