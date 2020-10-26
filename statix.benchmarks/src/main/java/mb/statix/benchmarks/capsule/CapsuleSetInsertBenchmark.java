package mb.statix.benchmarks.capsule;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import io.usethesource.capsule.Set;

@State(Scope.Thread)
public class CapsuleSetInsertBenchmark {

    @Param({ "100", "10000" }) private int number;

    @Benchmark @BenchmarkMode(Mode.All) public Set.Transient<Object> testAddTransient() {
        final Set.Transient<Object> set = Set.Transient.of();
        for(int i = 0; i < number; i++) {
            set.__insert(new Object());
        }
        return set;
    }

    @Benchmark @BenchmarkMode(Mode.All) public Set.Immutable<Object> testAddImmutable() {
        Set.Immutable<Object> set = Set.Immutable.of();
        for(int i = 0; i < number; i++) {
            set = set.__insert(new Object());
        }
        return set;
    }

}