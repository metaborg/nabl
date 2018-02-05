package org.metaborg.meta.nabl2.controlflow.terms;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

public final class ControlFlowGraphTerms {

    private static final String ESCAPE_MATCH = "\\\\$0";
    private static final String RECORD_RESERVED = "[\"{}|]";
    private final IControlFlowGraph<CFGNode> controlFlowGraph;

    private ControlFlowGraphTerms(IControlFlowGraph<CFGNode> controlFlowGraph) {
        this.controlFlowGraph = controlFlowGraph;
    }

    private ITerm build() {
        List<ITerm> directEdges = controlFlowGraph.getDirectEdges().entrySet().stream().map(this::buildDirectEdge)
                .collect(Collectors.toList());
        List<ITerm> starts = controlFlowGraph.getStartNodes().stream().collect(Collectors.toList());
        List<ITerm> ends = controlFlowGraph.getEndNodes().stream().collect(Collectors.toList());
        List<ITerm> properties = controlFlowGraph.getProperties().entrySet().stream().map(this::buildProperty)
                .collect(Collectors.toList());
        return TB.newAppl("ControlFlowGraph", (ITerm) TB.newList(directEdges), (ITerm) TB.newList(starts),
                (ITerm) TB.newList(ends), (ITerm) TB.newList(properties));
    }

    private ITerm buildDirectEdge(Map.Entry<CFGNode, CFGNode> directEdge) {
        return TB.newAppl("DirectEdge", directEdge.getKey(), directEdge.getValue());
    }

    private ITerm buildProperty(Map.Entry<Tuple2<CFGNode, String>, ITerm> directEdge) {
        return TB.newAppl("DFProperty", directEdge.getKey()._1(), TB.newString(directEdge.getKey()._2()), directEdge.getValue());
    }


    private String toDot() {
        Set<String> properties = controlFlowGraph.getProperties().keySet().stream().map(t -> t._2()).collect(Collectors.toSet());
        String starts = controlFlowGraph.getStartNodes().stream().map(node -> nodeToDot(node, properties)).collect(Collectors.joining());
        String ends = controlFlowGraph.getEndNodes().stream().map(node -> nodeToDot(node, properties)).collect(Collectors.joining());
        String otherNodes = controlFlowGraph.getNormalNodes().stream().map(node -> nodeToDot(node, properties)).collect(Collectors.joining());
        String edges = controlFlowGraph.getDirectEdges().tupleStream(this::edgeToDot).collect(Collectors.joining());
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
        ITerm propVal = controlFlowGraph.getProperty(node, prop);
        return "{" + prop.replaceAll(RECORD_RESERVED, ESCAPE_MATCH) + "|" + propVal.toString().replaceAll(RECORD_RESERVED, ESCAPE_MATCH) + "}";
    }

    public static ITerm build(IControlFlowGraph<CFGNode> controlFlowGraph) {
        return new ControlFlowGraphTerms(controlFlowGraph).build();
    }
    
    public static String toDot(IControlFlowGraph<CFGNode> controlFlowGraph) {
        return new ControlFlowGraphTerms(controlFlowGraph).toDot();
    }

}