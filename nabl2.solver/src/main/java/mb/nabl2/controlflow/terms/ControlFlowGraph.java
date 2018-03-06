package mb.nabl2.controlflow.terms;

import java.io.Serializable;

import org.immutables.value.Value;
import org.immutables.value.Value.Lazy;
import org.immutables.value.Value.Parameter;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Set;

@Value.Immutable
public abstract class ControlFlowGraph<N extends ICFGNode> implements IControlFlowGraph.Immutable<N>, Serializable {
    @Override
    @Lazy
    public Set.Immutable<N> nodes() {
        Set.Transient<N> allNodes = Set.Transient.of();
        allNodes.__insertAll(startNodes());
        allNodes.__insertAll(normalNodes());
        allNodes.__insertAll(artificialNodes());
        allNodes.__insertAll(endNodes());
        return allNodes.freeze();
    }

    @Override
    @Parameter
    public abstract BinaryRelation.Immutable<N, N> edges();

    @Override
    @Parameter
    public abstract Set.Immutable<N> startNodes();

    @Override
    @Parameter
    public abstract Set.Immutable<N> endNodes();

    @Override
    @Parameter
    public abstract Set.Immutable<N> normalNodes();

    @Override
    @Parameter
    public abstract Set.Immutable<N> artificialNodes();

    public static <N extends ICFGNode> IControlFlowGraph.Immutable<N> of() {
        return ImmutableControlFlowGraph.of(BinaryRelation.Immutable.of(), Set.Immutable.of(), Set.Immutable.of(),
                Set.Immutable.of(), Set.Immutable.of());
    }

    public IControlFlowGraph.Transient<N> asTransient() {
        return ImmutableTransientControlFlowGraph.of(edges().asTransient(), startNodes().asTransient(),
                endNodes().asTransient(), normalNodes().asTransient(),
                artificialNodes().asTransient());
    }
}
