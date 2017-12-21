package org.metaborg.meta.nabl2.controlflow.terms;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

public final class ControlFlowGraphTerms {

    private final IControlFlowGraph<CFGNode> controlFlowGraph;

    private ControlFlowGraphTerms(IControlFlowGraph<CFGNode> controlFlowGraph) {
        this.controlFlowGraph = controlFlowGraph;
    }

    private ITerm build() {
        List<ITerm> directEdges = controlFlowGraph.getDirectEdges().entrySet().stream().map(this::buildDirectEdge)
                .collect(Collectors.toList());
        List<ITerm> starts = controlFlowGraph.getAllStarts().stream().collect(Collectors.toList());
        List<ITerm> ends = controlFlowGraph.getAllEnds().stream().collect(Collectors.toList());
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

    // static interface

    public static ITerm build(IControlFlowGraph<CFGNode> controlFlowGraph) {
        return new ControlFlowGraphTerms(controlFlowGraph).build();
    }

}