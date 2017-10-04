package org.metaborg.meta.nabl2.controlflow.terms;

import com.google.common.collect.Lists;

import meta.flowspec.nabl2.controlflow.IControlFlowGraph;

import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.util.collections.IProperties;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.IUnifier;

import java.util.List;
import java.util.stream.Collectors;

public final class ControlFlowGraphTerms {

    private final IControlFlowGraph<CFGNode> controlFlowGraph;

    private ControlFlowGraphTerms(IControlFlowGraph<CFGNode> controlFlowGraph, IProperties<Occurrence, ITerm, ITerm> properties,
            IUnifier unifier) {
        this.controlFlowGraph = controlFlowGraph;
    }

    private ITerm build() {
        List<ITerm> CFGNodes = controlFlowGraph.getAllCFGNodes().stream().map(this::buildCFGNode).collect(Collectors.toList());
        return TB.newAppl("controlFlowGraph", (ITerm) TB.newList(CFGNodes));
    }

    private ITerm buildCFGNode(CFGNode cfgNode) {
        List<ITerm> parts = Lists.newArrayList();

        List<ITerm> directEdges =
                controlFlowGraph.getDirectEdges().get(cfgNode).stream().map(this::buildDirectEdge).collect(Collectors.toList());
        if(!directEdges.isEmpty()) {
            parts.add(TB.newAppl("DirectEdges", (ITerm) TB.newList(directEdges)));
        }

        return TB.newAppl("CFGNode", cfgNode, TB.newList(parts));
    }

    private ITerm buildDirectEdge(CFGNode target) {
        return TB.newAppl("DirectEdge", target);
    }

    // static interface

    public static ITerm build(IControlFlowGraph<CFGNode> controlFlowGraph, IProperties<Occurrence, ITerm, ITerm> properties,
            IUnifier unifier) {
        return new ControlFlowGraphTerms(controlFlowGraph, properties, unifier).build();
    }

}