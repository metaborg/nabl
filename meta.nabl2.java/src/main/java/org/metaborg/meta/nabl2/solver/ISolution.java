package org.metaborg.meta.nabl2.solver;

import java.util.Map;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.controlflow.terms.CFGNode;
import org.metaborg.meta.nabl2.relations.variants.IVariantRelation;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.symbolic.ISymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.util.collections.IProperties;

import meta.flowspec.nabl2.controlflow.IControlFlowGraph;

public interface ISolution extends IPublicSolution {

    @Override SolverConfig config();

    IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties();

    @Override IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

    @Override IEsopNameResolution.Immutable<Scope, Label, Occurrence> nameResolution();

    @Override IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties();

    @Override Map<String, IVariantRelation.Immutable<ITerm>> relations();

    @Override ISymbolicConstraints symbolic();
    
    @Override IControlFlowGraph<CFGNode> controlFlowGraph();

    IUnifier.Immutable unifier();

    IMessages.Immutable messages();

    java.util.Set<IConstraint> constraints();

    ISolution findAndLock() throws SolverException;

}