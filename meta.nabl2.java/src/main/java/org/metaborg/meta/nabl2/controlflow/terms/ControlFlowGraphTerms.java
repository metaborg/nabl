package org.metaborg.meta.nabl2.controlflow.terms;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Map.Immutable;

public final class ControlFlowGraphTerms {

    private static final String ESCAPE_MATCH = "\\\\$0";
    private static final String RECORD_RESERVED = "[\"{}|]";
    private final ICompleteControlFlowGraph<CFGNode> controlFlowGraph;
    private final Immutable<Tuple2<TermIndex, String>, ITerm> preProperties;
    private final Immutable<Tuple2<TermIndex, String>, ITerm> postProperties;

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
        return TB.newAppl("ControlFlowGraph", (ITerm) TB.newList(edges), (ITerm) TB.newList(starts),
                (ITerm) TB.newList(ends), (ITerm) TB.newList(properties));
    }

    private ITerm buildEdge(Map.Entry<CFGNode, CFGNode> directEdge) {
        return TB.newAppl("DirectEdge", directEdge.getKey(), directEdge.getValue());
    }

    private ITerm buildPreProperty(Map.Entry<Tuple2<TermIndex, String>, ITerm> directEdge) {
        return TB.newAppl("DFProperty", TB.newAppl("Pre", directEdge.getKey()._1()), TB.newString(directEdge.getKey()._2()), directEdge.getValue());
    }

    private ITerm buildPostProperty(Map.Entry<Tuple2<TermIndex, String>, ITerm> directEdge) {
        return TB.newAppl("DFProperty", TB.newAppl("Post", directEdge.getKey()._1()), TB.newString(directEdge.getKey()._2()), directEdge.getValue());
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
        String props = properties.stream().map(prop -> propertyToDot(node, prop)).collect(Collectors.joining("|"));
        return "\"" + node.toString() + "\" [ label = \"{" + (node.getName() + TermIndex.get(node).map(TermIndex::toString).orElse("")).replaceAll(RECORD_RESERVED, ESCAPE_MATCH) + "|" + props + "}\" ];\n";
    }

    // static interface

    private String propertyToDot(CFGNode node, String prop) {
        ITerm prePropVal = preProperties.get(ImmutableTuple2.of(TermIndex.get(node).get(), prop));
        ITerm postPropVal = postProperties.get(ImmutableTuple2.of(TermIndex.get(node).get(), prop));
        return "{" + prop.replaceAll(RECORD_RESERVED, ESCAPE_MATCH) + "|" + prePropVal.toString().replaceAll(RECORD_RESERVED, ESCAPE_MATCH) + " -> " + postPropVal.toString().replaceAll(RECORD_RESERVED, ESCAPE_MATCH) + "}";
    }

    public static ITerm build(IFlowSpecSolution<CFGNode> solution) {
        return new ControlFlowGraphTerms(solution).build();
    }

    public static String toDot(IFlowSpecSolution<CFGNode> solution) {
        return new ControlFlowGraphTerms(solution).toDot();
    }

}