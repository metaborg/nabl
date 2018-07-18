package mb.nabl2.controlflow.terms;

import org.immutables.value.Value;
import org.immutables.value.Value.Lazy;
import org.immutables.value.Value.Parameter;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Set;

@Value.Immutable
public abstract class TransientCompleteControlFlowGraph<N extends ICFGNode> implements ICompleteControlFlowGraph.Transient<N> {

    @Override
    @Parameter
    public abstract BinaryRelation.Transient<N, N> edges();

    @Override
    @Parameter
    public abstract Set.Transient<N> startNodes();

    @Override
    @Parameter
    public abstract Set.Transient<N> normalNodes();

    @Override
    @Parameter
    public abstract Set.Transient<N> endNodes();

    @Override
    @Parameter
    public abstract Set.Transient<N> entryNodes();

    @Override
    @Parameter
    public abstract Set.Transient<N> exitNodes();

    @Override
    @Lazy
    public Set.Immutable<N> nodes() {
        Set.Transient<N> allNodes = Set.Transient.of();
        allNodes.__insertAll(normalNodes());
        allNodes.__insertAll(startNodes());
        allNodes.__insertAll(endNodes());
        allNodes.__insertAll(entryNodes());
        allNodes.__insertAll(exitNodes());
        return allNodes.freeze();
    }

    public static <N extends ICFGNode> ICompleteControlFlowGraph.Transient<N> of() {
        return ImmutableTransientCompleteControlFlowGraph.of(BinaryRelation.Transient.of(), Set.Transient.of(),
                Set.Transient.of(), Set.Transient.of(), Set.Transient.of(), Set.Transient.of());
    }
}
