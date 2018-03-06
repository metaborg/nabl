package org.metaborg.meta.nabl2.controlflow.terms;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Set;

public interface IBasicControlFlowGraph<N extends ICFGNode> {
    static final ILogger logger = LoggerUtils.logger(IBasicControlFlowGraph.class);

    /**
     * @return true if the graph is empty; i.e. has no nodes. 
     */
    default boolean isEmpty() {
        return nodes().isEmpty();
    }

    /**
     * @return All nodes in the control flow graph(s). This may take some computation.
     */
    Set<N> nodes();

    /**
     * @return The edges of the control flow graph(s).
     */
    BinaryRelation<N, N> edges();

    /**
     * @return The start nodes of the control flow graph(s).
     */
    Set<N> startNodes();

    /**
     * @return All nodes that are not start or end nodes
     */
    Set<N> normalNodes();

    /**
     * @return The end nodes of the control flow graph(s).
     */
    Set<N> endNodes();

    interface Immutable<N extends ICFGNode> extends IBasicControlFlowGraph<N> {
        @Override Set.Immutable<N> nodes();
        @Override BinaryRelation.Immutable<N, N> edges();
        @Override Set.Immutable<N> startNodes();
        @Override Set.Immutable<N> normalNodes();
        @Override Set.Immutable<N> endNodes();

        /**
         * @return A completed control flow graph that has pre-computed SCCs and no artificial nodes
         */
        ICompleteControlFlowGraph.Immutable<N> asCompleteControlFlowGraph();
    }

    interface Transient<N extends ICFGNode> extends IBasicControlFlowGraph<N> {
        @Override BinaryRelation.Transient<N, N> edges();
        @Override Set.Transient<N> startNodes();
        @Override Set.Transient<N> normalNodes();
        @Override Set.Transient<N> endNodes();
    }
}