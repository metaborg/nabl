package mb.statix.benchmarks;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.persistent.State;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public abstract class JavaBenchmark {

    private static final String PROJECT_OK = "statics!projectOk";
    private static final String FILE_OK = "statics!fileOk";

    private Spec spec;
    private java.util.Map<String, Rule> units;
    private Tuple2<IState.Immutable, java.util.Map<String, IConstraint>> constraints;

    protected void doSetup() {
        try {
            this.spec = BenchmarkUtil.loadSpecFromATerm("metaborg-java.mergedspec.aterm");
            java.util.Map<String, ITerm> files = BenchmarkUtil.loadFilesFromATerm(name() + ".files.aterm");
            this.units = BenchmarkUtil.makeFileUnits(files, PROJECT_OK, FILE_OK);
            this.constraints = BenchmarkUtil.makeFileConstraints(State.of(spec), files, PROJECT_OK, FILE_OK);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    protected Object doRun(int parallelism) throws Exception, InterruptedException {
        if(parallelism == 0) {
            return BenchmarkUtil.runTraditional(spec, constraints._1(), constraints._2());
        } else {
            return BenchmarkUtil.runConcurrent(spec, units, parallelism);
        }
    }

    protected abstract String name();

}