package mb.statix.solver.persistent;

import java.util.Arrays;

import jakarta.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Function2;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.ud.PersistentUniDisunifier;
import mb.statix.constraints.Constraints;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.MessageKind;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.tracer.EmptyTracer.Empty;
import mb.statix.solver.tracer.SolverTracer;
import mb.statix.spec.Spec;

@Value.Immutable
@Serial.Version(42L)
public abstract class ASolverResult<TR extends SolverTracer.IResult<TR>> {

    @Value.Parameter public abstract Spec spec();

    @Value.Parameter public abstract IState.Immutable state();

    @Value.Parameter public abstract @Nullable TR traceResult();

    @Value.Parameter public abstract Map.Immutable<IConstraint, IMessage> messages();

    @Value.Parameter public abstract Map.Immutable<IConstraint, Delay> delays();

    @Value.Parameter public abstract Map.Immutable<ITermVar, ITermVar> existentials();

    @Value.Parameter public abstract Set.Immutable<ITermVar> updatedVars();

    @Value.Parameter public abstract Set.Immutable<CriticalEdge> removedEdges();

    @Value.Parameter public abstract ICompleteness.Immutable completeness();

    @Value.Default public int totalSolved() {
        return 0;
    }

    @Value.Default public int totalCriticalEdges() {
        return 0;
    }

    public boolean hasErrors() {
        return messages().values().stream().anyMatch(m -> m.kind().equals(MessageKind.ERROR));
    }

    public Delay delay() {
        return Delay.of(delays().values());
    }

    public IConstraint delayed() {
        return Constraints.conjoin(delays().keySet());
    }


    public static SolverResult<Empty> of(Spec spec) {
        return SolverResult.of(spec, State.of(), Empty.of(), CapsuleUtil.immutableMap(), CapsuleUtil.immutableMap(),
                CapsuleUtil.immutableMap(), CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet(),
                Completeness.Immutable.of());
    }

    public static <R extends SolverTracer.IResult<R>> SolverResult<R> of(Spec spec, @Nullable R tracerResult) {
        return SolverResult.of(spec, State.of(), tracerResult, CapsuleUtil.immutableMap(), CapsuleUtil.immutableMap(),
                CapsuleUtil.immutableMap(), CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet(),
                Completeness.Immutable.of());
    }

    public SolverResult<TR> combine(SolverResult<TR> other) {
        final SolverResult.Builder<TR> combined = SolverResult.<TR>builder().from(this);
        combined.state(state().add(other.state()));
        combined.messages(merge(messages(), other.messages()));
        combined.delays(merge(delays(), other.delays(), (d1, d2) -> {
            return Delay.of(Arrays.asList(d1, d2));
        }));
        combined.existentials(existentials().__putAll(other.existentials()));
        combined.updatedVars(updatedVars().__insertAll(other.updatedVars()));
        combined.removedEdges(removedEdges().__insertAll(other.removedEdges()));
        combined.completeness(completeness().addAll(other.completeness(), PersistentUniDisunifier.Immutable.of()));
        combined.totalSolved(totalSolved() + other.totalSolved());
        combined.totalCriticalEdges(totalCriticalEdges() + other.totalCriticalEdges());
        combined.traceResult(traceResult().combine(other.traceResult()));
        return combined.build();
    }

    private static <K, V> Map.Immutable<K, V> merge(Map.Immutable<K, V> map1, Map.Immutable<K, V> map2) {
        final Map.Transient<K, V> builder = CapsuleUtil.transientMap();
        builder.__putAll(map1);
        map2.forEach((k, v) -> {
            if(!map1.containsKey(k)) {
                builder.__put(k, v);
            }
        });
        return builder.freeze();
    }

    private static <K, V> Map.Immutable<K, V> merge(Map.Immutable<K, V> map1, Map.Immutable<K, V> map2, Function2<V, V, V> resolveConflict) {
        final Map.Transient<K, V> builder = CapsuleUtil.transientMap();
        builder.__putAll(map1);
        map2.forEach((k, v) -> {
            if(!map1.containsKey(k)) {
                builder.__put(k, v);
            } else {
                builder.__put(k, resolveConflict.apply(map1.get(k), v));
            }
        });
        return builder.freeze();
    }

}
