package org.metaborg.meta.nabl2.solver.components;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.controlflow.CFDirectEdge;
import org.metaborg.meta.nabl2.constraints.controlflow.IControlFlowConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.controlflow.terms.CFGNode;
import org.metaborg.meta.nabl2.controlflow.terms.ICompleteControlFlowGraph;
import org.metaborg.meta.nabl2.controlflow.terms.IControlFlowGraph;
import org.metaborg.meta.nabl2.controlflow.terms.TransientControlFlowGraph;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ISolver.SeedResult;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.terms.ITerm;

public class ControlFlowComponent extends ASolver {
    private final IControlFlowGraph.Transient<CFGNode> cfg;

    public ControlFlowComponent(SolverCore core, IControlFlowGraph.Immutable<CFGNode> cfg) {
        super(core);
        this.cfg = cfg.asTransient();
    }

    public ControlFlowComponent(SolverCore core, ICompleteControlFlowGraph.Immutable<CFGNode> cfg) {
        super(core);
        this.cfg = TransientControlFlowGraph.from(cfg);
    }

    public ICompleteControlFlowGraph.Immutable<CFGNode> finish() {
        return this.cfg.freeze().asCompleteControlFlowGraph();
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
            this.cfg.edges().__insert(sn, tn);
            return SolveResult.empty();
        }));
    }

    private Optional<CFGNode> findCFGNode(ITerm cfgNodeTerm) {
        return Optional.of(find(cfgNodeTerm)).filter(ITerm::isGround).map(
                st -> CFGNode.matcher().match(st).orElseThrow(() -> new TypeException("Expected a cfg node, got " + st)));
    }

    public SeedResult seed(ICompleteControlFlowGraph<CFGNode> cfg, IMessageInfo message) {
        this.cfg.addAll(cfg);
        return SeedResult.empty();
    }
}