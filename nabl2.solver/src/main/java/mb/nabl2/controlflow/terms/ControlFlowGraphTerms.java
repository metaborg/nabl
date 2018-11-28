package mb.nabl2.controlflow.terms;

import java.util.stream.Collectors;

import org.metaborg.util.Ref;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Map.Immutable;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public final class ControlFlowGraphTerms {

    private static final String ESCAPE_MATCH = "\\\\$0";
    private static final String RECORD_RESERVED = "[\"{}|]";
    private final ICompleteControlFlowGraph.Immutable<CFGNode> controlFlowGraph;
    private final Immutable<Tuple2<CFGNode, String>, Ref<ITerm>> preProperties;
    private final Immutable<Tuple2<CFGNode, String>, Ref<ITerm>> postProperties;

    private ControlFlowGraphTerms(IFlowSpecSolution<CFGNode> solution) {
        this.controlFlowGraph = solution.controlFlowGraph();
        this.preProperties = solution.preProperties();
        this.postProperties = solution.postProperties();
    }


    private String toDot() {
        final java.util.Set<String> properties = this.preProperties.keySet().stream().map(t -> t._2()).collect(Collectors.toSet());

        final Set.Immutable<CFGNode> entryExitNodes = Set.Immutable.union(controlFlowGraph.entryNodes(), controlFlowGraph.exitNodes());

        final String starts = controlFlowGraph.startNodes().stream().map(node -> nodeToDot(node, properties)).collect(Collectors.joining());
        final String ends = controlFlowGraph.endNodes().stream().map(node -> nodeToDot(node, properties)).collect(Collectors.joining());
        final String otherNodes = controlFlowGraph.normalNodes().stream().map(node -> nodeToDot(node, properties)).collect(Collectors.joining());
        final String edges = removeNodes(controlFlowGraph.edges(), entryExitNodes).tupleStream(this::edgeToDot).collect(Collectors.joining());

        return "digraph FG {\n"
             + "node [ shape = record, style = \"rounded\" ];\n"
             + starts
             + ends
             + "node [ style = \"\" ];\n"
             + otherNodes
             + edges
             + "}";
    }

    private BinaryRelation.Immutable<CFGNode, CFGNode> removeNodes(BinaryRelation.Immutable<CFGNode, CFGNode> edges, Set.Immutable<CFGNode> toRemove) {
        BinaryRelation.Transient<CFGNode, CFGNode> result = edges.asTransient();
        for(CFGNode node : toRemove) {
            Set.Immutable<CFGNode> tos = result.get(node);
            Set.Immutable<CFGNode> froms = result.inverse().get(node);
            result.__remove(node); // remove node ->> tos
            for(CFGNode from : froms) {
                result.__remove(from, node); // remove froms ->> node
                for(CFGNode to : tos) {
                    result.__put(from, to); // add froms -->> tos
                }
            }
        }
        return result.freeze();
    }

    private String edgeToDot(CFGNode from, CFGNode to) {
        return "\"" + from.toString() + "\" -> \"" + to.toString() + "\";\n";
    }

    private String nodeToDot(CFGNode node, java.util.Set<String> properties) {
        String propNames = "{" + properties.stream().map(n -> n.replaceAll(RECORD_RESERVED, ESCAPE_MATCH)).collect(Collectors.joining("|")) + "}";
        String prePropVals = "{" + properties.stream().map(n -> prePropValToDot(node, n)).collect(Collectors.joining("|")) + "}";
        String postPropVals = "{" + properties.stream().map(n -> postPropValToDot(node, n)).collect(Collectors.joining("|")) + "}";
        final String props = "{" + propNames + "|" + prePropVals + "|" + postPropVals + "}";
        return "\"" + node.toString() + "\" [ label = \"{" + (node.getName() + node.getIndex().toString()).replaceAll(RECORD_RESERVED, ESCAPE_MATCH) + "|" + props + "}\" ];\n";
    }

    private String prePropValToDot(CFGNode node, String prop) {
        ITerm prePropVal = preProperties.get(ImmutableTuple2.of(node, prop)).get();
        return prePropVal.toString().replaceAll(RECORD_RESERVED, ESCAPE_MATCH);
    }

    private String postPropValToDot(CFGNode node, String prop) {
        ITerm postPropVal = postProperties.get(ImmutableTuple2.of(node, prop)).get();
        return postPropVal.toString().replaceAll(RECORD_RESERVED, ESCAPE_MATCH);
    }

    // static interface

    public static String toDot(IFlowSpecSolution<CFGNode> solution) {
        return new ControlFlowGraphTerms(solution).toDot();
    }

}
