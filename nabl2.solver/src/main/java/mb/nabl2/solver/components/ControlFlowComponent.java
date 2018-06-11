package mb.nabl2.solver.components;

import java.util.Optional;

import io.usethesource.capsule.Map;
import mb.nabl2.constraints.controlflow.CFDirectEdge;
import mb.nabl2.constraints.controlflow.CTFAppl;
import mb.nabl2.constraints.controlflow.IControlFlowConstraint;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.controlflow.terms.CFGNode;
import mb.nabl2.controlflow.terms.IControlFlowGraph;
import mb.nabl2.controlflow.terms.IFlowSpecSolution;
import mb.nabl2.controlflow.terms.ImmutableFlowSpecSolution;
import mb.nabl2.controlflow.terms.ImmutableTransferFunctionAppl;
import mb.nabl2.controlflow.terms.ImmutableTransientControlFlowGraph;
import mb.nabl2.controlflow.terms.TransferFunctionAppl;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.ISolver.SeedResult;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.solver.TypeException;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public class ControlFlowComponent extends ASolver {
    private final IControlFlowGraph.Transient<CFGNode> cfg;
    private final Map.Transient<Tuple2<CFGNode, String>, TransferFunctionAppl> tfAppls;

    public ControlFlowComponent(SolverCore core, IFlowSpecSolution<CFGNode> solution) {
        super(core);
        this.cfg = ImmutableTransientControlFlowGraph.of();
        this.cfg.addAll(solution.controlFlowGraph());
        this.tfAppls = solution.tfAppls().asTransient();
    }

    public IFlowSpecSolution<CFGNode> finish() {
        return ImmutableFlowSpecSolution.of(this.cfg.freeze().asCompleteControlFlowGraph(), tfAppls.freeze());
    }

    public void update() throws InterruptedException {
    }

    public Optional<SolveResult> solve(IControlFlowConstraint constraint) {
        return constraint.match(IControlFlowConstraint.Cases.of(this::solve, this::solve));
    }

    private Optional<SolveResult> solve(CFDirectEdge<?> c) {
        Optional<CFGNode> sourceNode = findCFGNode(c.getSourceNode());
        Optional<CFGNode> targetNode = findCFGNode(c.getTargetNode());

        return sourceNode.flatMap(sn -> targetNode.map(tn -> {
            this.addCFGNode(sn);
            this.addCFGNode(tn);
            this.cfg.edges().__insert(sn, tn);
            return SolveResult.empty();
        }));
    }

    private void addCFGNode(CFGNode node) {
        switch (node.getKind()) {
        case Artificial:
            cfg.artificialNodes().__insert(node);
            break;
        case End:
            cfg.endNodes().__insert(node);
            break;
        case Normal:
            cfg.normalNodes().__insert(node);
            break;
        case Start:
            cfg.startNodes().__insert(node);
            break;
        }
    }

    private Optional<SolveResult> solve(CTFAppl c) {
        tfAppls.__put(ImmutableTuple2.of(c.getCFGNode(), c.getPropertyName()), ImmutableTransferFunctionAppl.of(c.getOffset(), c.getArguments()));
        return Optional.ofNullable(SolveResult.empty());
    }

    private Optional<CFGNode> findCFGNode(ITerm cfgNodeTerm) {
        return Optional.of(cfgNodeTerm).filter(unifier()::isGround).map(
                st -> CFGNode.matcher().match(st, unifier()).orElseThrow(() -> new TypeException("Expected a cfg node, got " + st)));
    }

    public SeedResult seed(IFlowSpecSolution<CFGNode> solution, IMessageInfo message) {
        this.cfg.addAll(solution.controlFlowGraph());
        this.tfAppls.__putAll(solution.tfAppls());
        return SeedResult.empty();
    }
}
