package org.metaborg.meta.nabl2.solver.components;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.controlflow.CFDecl;
import org.metaborg.meta.nabl2.constraints.controlflow.CFDeclProperty;
import org.metaborg.meta.nabl2.constraints.controlflow.CFDirectEdge;
import org.metaborg.meta.nabl2.constraints.controlflow.IControlFlowConstraint;
import org.metaborg.meta.nabl2.constraints.controlflow.IControlFlowConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.controlflow.ImmutableCFDirectEdge;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;

import meta.flowspec.nabl2.controlflow.IControlFlowGraph;
import meta.flowspec.nabl2.controlflow.impl.ControlFlowGraph;
import org.metaborg.meta.nabl2.controlflow.terms.CFGNode;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.solver.IProperties;
import org.metaborg.meta.nabl2.solver.Properties;
import org.metaborg.meta.nabl2.solver.Solver;
import org.metaborg.meta.nabl2.solver.SolverComponent;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.solver.UnsatisfiableException;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.IIntTerm;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.IStringTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

import meta.flowspec.java.interpreter.TransferFunctionAppl;

public class ControlFlowSolver extends SolverComponent<IControlFlowConstraint> {
    private static final ILogger logger = LoggerUtils.logger(ControlFlowSolver.class);

    private final ControlFlowGraph<CFGNode> controlFlowGraph;
    private final Properties<Occurrence> properties;

    private final Set<IControlFlowConstraint> unsolvedBuilds;
    private final Set<CFDirectEdge<CFGNode>> incompleteDirectEdges;
    private final Set<IControlFlowConstraint> unsolvedChecks;

    public ControlFlowSolver(Solver solver) {
        super(solver);
        this.controlFlowGraph = new ControlFlowGraph<>();
        this.properties = new Properties<>();

        this.unsolvedBuilds = Sets.newHashSet();
        this.incompleteDirectEdges = Sets.newHashSet();
        this.unsolvedChecks = Sets.newHashSet();
    }

    public IControlFlowGraph<CFGNode> getControlFlowGraph() {
        return controlFlowGraph;
    }

    public IProperties<Occurrence> getProperties() {
        return properties;
    }

    @Override
    protected Unit doAdd(IControlFlowConstraint constraint) throws UnsatisfiableException {
        logger.debug("ControlFlowSolver::doAdd");
        return constraint.matchOrThrow(CheckedCases.of(this::addBuild, this::addBuild, this::add));
    }

    @Override
    protected boolean doIterate() throws UnsatisfiableException, InterruptedException {
        logger.debug("ControlFlowSolver::doIterate");
        boolean progress = doIterate(unsolvedBuilds, this::solve);
        progress |= doIterate(incompleteDirectEdges, this::solveDirectEdge);
        progress |= doIterate(unsolvedChecks, this::solve);
        return progress;
    }

    @Override
    protected Set<? extends IControlFlowConstraint> doFinish(IMessageInfo messageInfo) {
        return Sets.newHashSet();
    }

    @SuppressWarnings("SameReturnValue")
    private Unit addBuild(IControlFlowConstraint constraint) throws UnsatisfiableException {
        logger.debug("Adding ControlFlowConstraint");
        if (!solve(constraint)) {
            unsolvedBuilds.add(constraint);
        } else {
            work();
        }
        return unit;
    }

    @SuppressWarnings("SameReturnValue")
    private Unit add(CFDeclProperty constraint) throws UnsatisfiableException {
        logger.debug("Adding Declaration Property");
        unifier().addActive(constraint.getValue(), constraint);
        if (!solve(constraint)) {
            unsolvedChecks.add(constraint);
        } else {
            work();
        }
        return unit;
    }

    private boolean solve(IControlFlowConstraint constraint) throws UnsatisfiableException {
        logger.debug("ControlFlowSolver::solve");
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve, this::solve));
    }

    private boolean solve(CFDecl c) {
        logger.debug("Solve Declaration");
        ITerm nodeTerm = unifier().find(c.getNode());
        ITerm declTerm = unifier().find(c.getDeclaration());
        if (!(nodeTerm.isGround() && declTerm.isGround())) {
            return false;
        }
        CFGNode node = CFGNode.matcher().match(nodeTerm)
                .orElseThrow(() -> new TypeException("Expected a node as first argument to " + c));
        Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as second argument to " + c));
        switch(decl.getNamespace().getName()) {
            case "TF": {
                ITerm listTerm = decl.getName();
                if (!listTerm.isGround()) {
                    return false;
                }
                if (!(listTerm instanceof IListTerm)) {
                    throw new TypeException("Expected occurence in CFDecl to have a list as a \"name\"");
                }
                for(ITerm pairTerm : (IListTerm) listTerm) {
                    Tuple2<String, TransferFunctionAppl> result = matchStringTFApplPair(pairTerm);
                    String prop = result._1();
                    TransferFunctionAppl number = result._2();
                    controlFlowGraph.addTFAppl(node, prop, number);
                }
                break;
            }
            case "AST": {
                break;
            }
            default: {
                throw new TypeException("Expected occurrence in CFDecl to have namespace TF or AST but was: " + decl.getNamespace().getName());
            }
        }
        return true;
    }

    private Tuple2<String, TransferFunctionAppl> matchStringTFApplPair(ITerm pairTerm) {
        if (!(pairTerm instanceof IApplTerm)) {
            throw new TypeException("Expected occurence in CFDecl to have a list of applications as a \"name\"");
        }
        IApplTerm pair = (IApplTerm) pairTerm;
        if (pair.getOp() != "" || pair.getArity() != 2) {
            throw new TypeException("Expected occurence in CFDecl to have a list of (_,_) as a \"name\"");
        }
        ITerm propTerm = pair.getArgs().get(0);
        ITerm pair2Term = pair.getArgs().get(1);
        if (!(propTerm instanceof IStringTerm)) {
            throw new TypeException("Expected occurence in CFDecl to have a list of (String, _) as a \"name\"");
        }
        if (!(pair2Term instanceof IApplTerm)) {
            throw new TypeException("Expected occurence in CFDecl to have a list of (String, appl) as a \"name\"");
        }
        IApplTerm pair2 = (IApplTerm) pair2Term;
        if(pair2.getOp() != "" || pair2.getArity() != 2) {
            throw new TypeException("Expected occurence in CFDecl to have a list of (String, (_,_)) as a \"name\"");
        }
        ITerm intTerm = pair2.getArgs().get(0);
        ITerm argsTerm = pair2.getArgs().get(1);
        if (!(intTerm instanceof IIntTerm)) {
            throw new TypeException("Expected occurence in CFDecl to have a list of (String, (int, _)) as a \"name\"");
        }
        if (!(argsTerm instanceof IListTerm)) {
            throw new TypeException("Expected occurence in CFDecl to have a list of (String, (int, [...])) as a \"name\"");
        }
        ITerm[] args = Iterators.toArray(((IListTerm) argsTerm).iterator(), ITerm.class);
        return ImmutableTuple2.of(
                ((IStringTerm) propTerm).getValue(), 
                new TransferFunctionAppl(((IIntTerm) intTerm).getValue(), args));
    }

    private boolean solve(CFDirectEdge<?> c) {
        logger.debug("Solve Directed Edge");
        ITerm sourceNodeTerm = unifier().find(c.getSourceNode());
        if (!sourceNodeTerm.isGround()) {
            return false;
        }
        CFGNode node = CFGNode.matcher().match(sourceNodeTerm)
                .orElseThrow(() -> new TypeException("Expected a node as first argument to " + c));
        CFDirectEdge<CFGNode> cc = ImmutableCFDirectEdge.of(node, c.getTargetNode(), c.getMessageInfo());
        if (!solveDirectEdge(cc)) {
            incompleteDirectEdges.add(cc);
        }
        return true;
    }

    private boolean solveDirectEdge(CFDirectEdge<CFGNode> c) {
        logger.debug("ControlFlowSolver::solveDirectedEdge");
        ITerm targetNodeTerm = unifier().find(c.getTargetNode());
        if (!targetNodeTerm.isGround()) {
            return false;
        }
        CFGNode targetNode = CFGNode.matcher().match(targetNodeTerm)
                .orElseThrow(() -> new TypeException("Expected a node as third argument to " + c));
        controlFlowGraph.addDirectEdge(c.getSourceNode(), targetNode);
        return true;
    }

    private boolean solve(CFDeclProperty c) throws UnsatisfiableException {
        logger.debug("Solve Declaration Property");
        ITerm declTerm = unifier().find(c.getDeclaration());
        if (!declTerm.isGround()) {
            return false;
        }
        Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        unifier().removeActive(c.getValue(), c); // before `unify`, so that we
                                                 // don't cause an error chain
                                                 // if that fails
        Optional<ITerm> prev = properties.putValue(decl, c.getKey(), c.getValue());
        if (prev.isPresent()) {
            try {
                unifier().unify(c.getValue(), prev.get());
            } catch (UnificationException ex) {
                throw new UnsatisfiableException(c.getMessageInfo().withDefaultContent(ex.getMessageContent()));
            }
        }
        return true;
    }
}