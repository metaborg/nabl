package mb.nabl2.controlflow.terms;

import java.util.Set;
import java.util.stream.Collectors;

import io.usethesource.capsule.Map.Immutable;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public final class ControlFlowGraphTerms {

    private static final String ESCAPE_MATCH = "\\\\$0";
    private static final String RECORD_RESERVED = "[\"{}|]";
    private final ICompleteControlFlowGraph<CFGNode> controlFlowGraph;
    private final Immutable<Tuple2<CFGNode, String>, ITerm> preProperties;
    private final Immutable<Tuple2<CFGNode, String>, ITerm> postProperties;

    private ControlFlowGraphTerms(IFlowSpecSolution<CFGNode> solution) {
        this.controlFlowGraph = solution.controlFlowGraph();
        this.preProperties = solution.preProperties();
        this.postProperties = solution.postProperties();
    }


    private String toDot() {
        Set<String> properties = this.preProperties.keySet().stream().map(t -> t._2()).collect(Collectors.toSet());
        String starts = controlFlowGraph.startNodes().stream().map(node -> nodeToDot(node, properties)).collect(Collectors.joining());
        String ends = controlFlowGraph.endNodes().stream().map(node -> nodeToDot(node, properties)).collect(Collectors.joining());
        String otherNodes = controlFlowGraph.normalNodes().stream().map(node -> nodeToDot(node, properties)).collect(Collectors.joining());
        String edges = controlFlowGraph.edges().tupleStream(this::edgeToDot).collect(Collectors.joining());
        return "digraph FG {\n"
             + "node [ shape = record, style = \"rounded\" ];\n"
             + starts
             + ends
             + "node [ style = \"\" ];\n"
             + otherNodes
             + edges
             + "}";
    }

    private String edgeToDot(CFGNode from, CFGNode to) {
        return "\"" + from.toString() + "\" -> \"" + to.toString() + "\";\n";
    }

    private String nodeToDot(CFGNode node, Set<String> properties) {
        String propNames = "{" + properties.stream().map(n -> n.replaceAll(RECORD_RESERVED, ESCAPE_MATCH)).collect(Collectors.joining("|")) + "}";
        String prePropVals = "{" + properties.stream().map(n -> prePropValToDot(node, n)).collect(Collectors.joining("|")) + "}";
        String postPropVals = "{" + properties.stream().map(n -> postPropValToDot(node, n)).collect(Collectors.joining("|")) + "}";
        final String props = "{" + propNames + "|" + prePropVals + "|" + postPropVals + "}";
        return "\"" + node.toString() + "\" [ label = \"{" + (node.getName() + node.getIndex().toString()).replaceAll(RECORD_RESERVED, ESCAPE_MATCH) + "|" + props + "}\" ];\n";
    }

    private String prePropValToDot(CFGNode node, String prop) {
        ITerm prePropVal = preProperties.get(ImmutableTuple2.of(node, prop));
        return prePropVal.toString().replaceAll(RECORD_RESERVED, ESCAPE_MATCH);
    }

    private String postPropValToDot(CFGNode node, String prop) {
        ITerm postPropVal = postProperties.get(ImmutableTuple2.of(node, prop));
        return postPropVal.toString().replaceAll(RECORD_RESERVED, ESCAPE_MATCH);
    }

    // static interface

    public static String toDot(IFlowSpecSolution<CFGNode> solution) {
        return new ControlFlowGraphTerms(solution).toDot();
    }

}
