package org.metaborg.meta.nabl2.solver.components;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.controlflow.CFDecl;
import org.metaborg.meta.nabl2.constraints.controlflow.CFDeclProperty;
import org.metaborg.meta.nabl2.constraints.controlflow.CFDirectEdge;
import org.metaborg.meta.nabl2.constraints.controlflow.IControlFlowConstraint;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.controlflow.terms.CFGNode;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.IIntTerm;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.IStringTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ListTerms;
import org.metaborg.meta.nabl2.terms.generic.ListTermIterator;
import org.metaborg.meta.nabl2.util.collections.IProperties;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Iterators;

import meta.flowspec.java.interpreter.TransferFunctionAppl;
import meta.flowspec.nabl2.controlflow.IControlFlowGraph;
import meta.flowspec.nabl2.controlflow.impl.ControlFlowGraph;

public class ControlFlowComponent extends ASolver {
    private static final ILogger logger = LoggerUtils.logger(ControlFlowComponent.class);

    private final ControlFlowGraph<CFGNode> controlFlowGraph;

    private IProperties.Transient<Occurrence, ITerm, ITerm> properties;

    public ControlFlowComponent(SolverCore core, ControlFlowGraph<CFGNode> cfg,
            IProperties.Transient<Occurrence, ITerm, ITerm> initial) {
        super(core);
        this.controlFlowGraph = cfg;
        this.properties = initial;
    }

    public IControlFlowGraph<CFGNode> getControlFlowGraph() {
        return controlFlowGraph;
    }

    public void update() throws InterruptedException {
        controlFlowGraph.reduce(this::findCFGNode);
    }

    public Optional<SolveResult> solve(IControlFlowConstraint constraint) {
        logger.debug("ControlFlowSolver::solve");
        return constraint.match(IControlFlowConstraint.Cases.of(this::solve, this::solve, this::solve));
    }

    private Optional<SolveResult> solve(CFDecl c) {
        logger.debug("Solve Declaration");
        ITerm nodeTerm = find(c.getNode());
        ITerm declTerm = find(c.getDeclaration());
        if (!(nodeTerm.isGround() && declTerm.isGround())) {
            return Optional.empty();
        }
        CFGNode node = CFGNode.matcher().match(nodeTerm)
                .orElseThrow(() -> new TypeException("Expected a node as first argument to " + c));
        Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as second argument to " + c));
        switch (decl.getNamespace().getName()) {
        case "TF": {
            ITerm listTerm = decl.getName();
            if (!listTerm.isGround()) {
                return Optional.empty();
            }
            if (!(listTerm instanceof IListTerm)) {
                throw new TypeException("Expected occurence in CFDecl to have a list as a \"name\"");
            }
            for (ITerm pairTerm : ListTerms.iterable((IListTerm) listTerm)) {
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
            throw new TypeException("Expected occurrence in CFDecl to have namespace TF or AST but was: "
                    + decl.getNamespace().getName());
        }
        }
        return Optional.of(SolveResult.empty());
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
        if (pair2.getOp() != "" || pair2.getArity() != 2) {
            throw new TypeException("Expected occurence in CFDecl to have a list of (String, (_,_)) as a \"name\"");
        }
        ITerm intTerm = pair2.getArgs().get(0);
        ITerm argsTerm = pair2.getArgs().get(1);
        if (!(intTerm instanceof IIntTerm)) {
            throw new TypeException("Expected occurence in CFDecl to have a list of (String, (int, _)) as a \"name\"");
        }
        if (!(argsTerm instanceof IListTerm)) {
            throw new TypeException(
                    "Expected occurence in CFDecl to have a list of (String, (int, [...])) as a \"name\"");
        }
        ITerm[] args = Iterators.toArray(new ListTermIterator((IListTerm) argsTerm), ITerm.class);
        return ImmutableTuple2.of(((IStringTerm) propTerm).getValue(),
                new TransferFunctionAppl(((IIntTerm) intTerm).getValue(), args));
    }

    private Optional<SolveResult> solve(CFDirectEdge<?> c) {
        logger.debug("Solve Directed Edge");
        Optional<CFGNode> sourceNode = findCFGNode(c.getSourceNode());
        Optional<CFGNode> targetNode = findCFGNode(c.getTargetNode());

        return sourceNode.flatMap(sn -> targetNode.map(tn -> {
            controlFlowGraph.addDirectEdge(sn, tn);
            return SolveResult.empty();
        }));
    }

    private Optional<SolveResult> solve(CFDeclProperty c) {
        logger.debug("Solve Declaration Property");
        Optional<Occurrence> declTerm = findOccurrence(c.getDeclaration());

        return declTerm.map(decl -> 
            putProperty(decl, c.getKey(), c.getValue(), c.getMessageInfo())
                .map(cc -> SolveResult.constraints(cc)).orElseGet(() -> SolveResult.empty())
        );
    }

    private Optional<IConstraint> putProperty(Occurrence decl, ITerm key, ITerm value, IMessageInfo message) {
        Optional<ITerm> prev = properties.getValue(decl, key);
        if(!prev.isPresent()) {
            properties.putValue(decl, key, value);
            return Optional.empty();
        } else {
            return Optional.of(ImmutableCEqual.of(value, prev.get(), message));
        }
    }

    private Optional<CFGNode> findCFGNode(ITerm cfgNodeTerm) {
        return Optional.of(find(cfgNodeTerm)).filter(ITerm::isGround).map(
                st -> CFGNode.matcher().match(st).orElseThrow(() -> new TypeException("Expected a scope, got " + st)));
    }

    private Optional<Occurrence> findOccurrence(ITerm occurrenceTerm) {
        return Optional.of(find(occurrenceTerm)).filter(ITerm::isGround).map(ot -> Occurrence.matcher().match(ot)
                .orElseThrow(() -> new TypeException("Expected an occurrence, got " + ot)));
    }
}