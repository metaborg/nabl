package org.metaborg.meta.nabl2.solver.components;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.controlflow.CFDirectEdge;
import org.metaborg.meta.nabl2.constraints.controlflow.IControlFlowConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.controlflow.terms.CFGNode;
import org.metaborg.meta.nabl2.controlflow.terms.ControlFlowGraph;
import org.metaborg.meta.nabl2.controlflow.terms.IControlFlowGraph;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ISolver.SeedResult;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.terms.ITerm;

public class ControlFlowComponent extends ASolver {
    private final ControlFlowGraph<CFGNode> controlFlowGraph;

    public ControlFlowComponent(SolverCore core, ControlFlowGraph<CFGNode> cfg) {
        super(core);
        this.controlFlowGraph = cfg;
    }

    public IControlFlowGraph<CFGNode> getControlFlowGraph() {
        return controlFlowGraph;
    }

    public void update() throws InterruptedException {
    }

    public Optional<SolveResult> solve(IControlFlowConstraint constraint) {
        return constraint.match(IControlFlowConstraint.Cases.of(this::solve));
    }

    private Optional<SolveResult> solve(CFDirectEdge<?> c) {
        Optional<CFGNode> sourceNode = findCFGNode(c.getSourceNode());
        Optional<CFGNode> targetNode = findCFGNode(c.getTargetNode());

        return sourceNode.flatMap(sn -> targetNode.map(tn -> {
            controlFlowGraph.addDirectEdge(sn, tn);
            return SolveResult.empty();
        }));
    }

    private Optional<CFGNode> findCFGNode(ITerm cfgNodeTerm) {
        return Optional.of(unifier().findRecursive(cfgNodeTerm)).filter(ITerm::isGround).map(
                st -> CFGNode.matcher().match(st, unifier()).orElseThrow(() -> new TypeException("Expected a cfg node, got " + st)));
    }

    public SeedResult seed(IControlFlowGraph<CFGNode> solution, IMessageInfo message) {
        controlFlowGraph.addAll(solution);
        return SeedResult.empty();
    }
}