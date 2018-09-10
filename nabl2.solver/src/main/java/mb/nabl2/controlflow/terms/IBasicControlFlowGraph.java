package mb.nabl2.controlflow.terms;

import java.util.Optional;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.stratego.TermIndex;

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
     * @return The end nodes of the control flow graph(s).
     */
    Set<N> endNodes();

    /**
     * @return The entry nodes of the control flow graph(s).
     */
    Set<N> entryNodes();

    /**
     * @return The exit nodes of the control flow graph(s).
     */
    Set<N> exitNodes();

    /**
     * @return All nodes that are not start or end nodes
     */
    Set<N> normalNodes();

    interface Immutable<N extends ICFGNode> extends IBasicControlFlowGraph<N> {
        @Override Set.Immutable<N> nodes();
        @Override BinaryRelation.Immutable<N, N> edges();
        @Override Set.Immutable<N> startNodes();
        @Override Set.Immutable<N> normalNodes();
        @Override Set.Immutable<N> endNodes();
        @Override Set.Immutable<N> entryNodes();
        @Override Set.Immutable<N> exitNodes();
        
        default Map.Immutable<TermIndex, N> startNodeMap() {
            Map.Transient<TermIndex, N> map = Map.Transient.of();
            startNodes().stream().forEach(node -> {
                map.__put(node.getIndex(), node);
            });
            return map.freeze();
        }
        
        default Map.Immutable<TermIndex, N> endNodeMap() {
            Map.Transient<TermIndex, N> map = Map.Transient.of();
            endNodes().stream().forEach(node -> {
                map.__put(node.getIndex(), node);
            });
            return map.freeze();
        }
        
        default Map.Immutable<TermIndex, N> entryNodeMap() {
            Map.Transient<TermIndex, N> map = Map.Transient.of();
            entryNodes().stream().forEach(node -> {
                map.__put(node.getIndex(), node);
            });
            return map.freeze();
        }
        
        default Map.Immutable<TermIndex, N> exitNodeMap() {
            Map.Transient<TermIndex, N> map = Map.Transient.of();
            exitNodes().stream().forEach(node -> {
                map.__put(node.getIndex(), node);
            });
            return map.freeze();
        }
        
        default Map.Immutable<TermIndex, N> normalNodeMap() {
            Map.Transient<TermIndex, N> map = Map.Transient.of();
            normalNodes().stream().forEach(node -> {
                map.__put(node.getIndex(), node);
            });
            return map.freeze();
        }

        /**
         * @return Find the CFG node associated with the following TermIndex, of the right kind
         */
        default Optional<N> findNode(TermIndex index, ICFGNode.Kind kind) {
            final Map<TermIndex, N> map;
            switch(kind) {
                case Normal:
                    map = normalNodeMap();
                    break;
                case Start:
                    map = startNodeMap();
                    break;
                case End:
                    map = endNodeMap();
                    break;
                case Entry:
                    map = entryNodeMap();
                    break;
                case Exit:
                    map = exitNodeMap();
                    break;
                default:
                    map = Map.Immutable.of();
                    break;
            }
            return Optional.ofNullable(map.get(index));
        }
    }

    interface Transient<N extends ICFGNode> extends IBasicControlFlowGraph<N> {
        @Override BinaryRelation.Transient<N, N> edges();
        @Override Set.Transient<N> startNodes();
        @Override Set.Transient<N> normalNodes();
        @Override Set.Transient<N> endNodes();
        @Override Set.Transient<N> entryNodes();
        @Override Set.Transient<N> exitNodes();
    }
}