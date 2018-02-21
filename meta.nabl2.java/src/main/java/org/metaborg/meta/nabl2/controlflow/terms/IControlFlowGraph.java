package org.metaborg.meta.nabl2.controlflow.terms;

import java.util.Map.Entry;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Set;

public interface IControlFlowGraph<N extends ICFGNode> extends IBasicControlFlowGraph<N> {
    /**
     * @return All artificial nodes
     */
    Set<N> artificialNodes();
    
    interface Immutable<N extends ICFGNode> extends IControlFlowGraph<N>, IBasicControlFlowGraph.Immutable<N> {
        @Override Set.Immutable<N> nodes();
        @Override BinaryRelation.Immutable<N, N> edges();
        @Override Set.Immutable<N> startNodes();
        @Override Set.Immutable<N> normalNodes();
        @Override Set.Immutable<N> endNodes();

        @Override
        default ICompleteControlFlowGraph.Immutable<N> asCompleteControlFlowGraph() {
            BinaryRelation.Transient<N, N> edges = edges().asTransient();

            for (N n : artificialNodes()) {
                Set.Immutable<N> to = edges.get(n);
                Set.Immutable<N> from = edges.inverse().get(n);

                edges.__remove(n);
                for (N f : from) {
                    edges.__remove(f, n);
                    edges.__insert(f, to);
                }
            }

            return CompleteControlFlowGraph.of(normalNodes(), edges.freeze(), startNodes(), endNodes());
        }

        @Override Set.Immutable<N> artificialNodes();
        IControlFlowGraph.Transient<N> asTransient();
    }

    interface Transient<N extends ICFGNode> extends IControlFlowGraph<N>, IBasicControlFlowGraph.Transient<N> {
        @Override BinaryRelation.Transient<N, N> edges();
        @Override Set.Transient<N> startNodes();
        @Override Set.Transient<N> normalNodes();
        @Override Set.Transient<N> endNodes();
        Set.Transient<N> artificialNodes();

        default boolean addAll(IControlFlowGraph<N> other) {
            boolean change = false;
            for (Entry<N, N> e : other.edges().entrySet()) {
                change |= edges().__insert(e.getKey(), e.getValue());
            }
            change |= startNodes().__insertAll(other.startNodes());
            change |= normalNodes().__insertAll(other.normalNodes());
            change |= endNodes().__insertAll(other.endNodes());
            change |= artificialNodes().__insertAll(other.artificialNodes());
            return change;
        }

        default boolean addAll(ICompleteControlFlowGraph<N> other) {
            boolean change = false;
            for (Entry<N, N> e : other.edges().entrySet()) {
                change |= edges().__insert(e.getKey(), e.getValue());
            }
            change |= startNodes().__insertAll(other.startNodes());
            change |= normalNodes().__insertAll(other.normalNodes());
            change |= endNodes().__insertAll(other.endNodes());
            return change;
        }
        
        IControlFlowGraph.Immutable<N> freeze();
    }
}
