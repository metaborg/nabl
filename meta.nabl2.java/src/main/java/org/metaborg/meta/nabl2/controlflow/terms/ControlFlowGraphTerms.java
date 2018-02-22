package org.metaborg.meta.nabl2.controlflow.terms;

import static org.metaborg.meta.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.Tuple2;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Map.Immutable;

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

    private ITerm build() {
        List<ITerm> edges = controlFlowGraph.edges().entrySet().stream().map(this::buildEdge)
                .collect(Collectors.toList());
        List<ITerm> starts = controlFlowGraph.startNodes().stream().collect(Collectors.toList());
        List<ITerm> ends = controlFlowGraph.endNodes().stream().collect(Collectors.toList());
        List<ITerm> properties = this.preProperties.entrySet().stream().map(this::buildPreProperty)
                .collect(Collectors.toList());
        List<ITerm> postProperties = this.postProperties.entrySet().stream().map(this::buildPostProperty)
                .collect(Collectors.toList());
        properties.addAll(postProperties);
        return B.newAppl("ControlFlowGraph", (ITerm) B.newList(edges), (ITerm) B.newList(starts),
                (ITerm) B.newList(ends), (ITerm) B.newList(properties));
    }

    private ITerm buildEdge(Map.Entry<CFGNode, CFGNode> directEdge) {
        return B.newAppl("DirectEdge", directEdge.getKey(), directEdge.getValue());
    }

    private ITerm buildPreProperty(Map.Entry<Tuple2<CFGNode, String>, ITerm> directEdge) {
        return B.newAppl("DFProperty", B.newAppl("Pre", directEdge.getKey()._1()), B.newString(directEdge.getKey()._2()), directEdge.getValue());
    }

    private ITerm buildPostProperty(Map.Entry<Tuple2<CFGNode, String>, ITerm> directEdge) {
        return B.newAppl("DFProperty", B.newAppl("Post", directEdge.getKey()._1()), B.newString(directEdge.getKey()._2()), directEdge.getValue());
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

    public static ITerm build(IFlowSpecSolution<CFGNode> solution) {
        return new ControlFlowGraphTerms(solution).build();
    }

    public static String toDot(IFlowSpecSolution<CFGNode> solution) {
        return new ControlFlowGraphTerms(solution).toDot();
    }

}
