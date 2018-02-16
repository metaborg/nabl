package org.metaborg.meta.nabl2.solver;

import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.IConstraint;
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
import org.metaborg.meta.nabl2.terms.unification.IUnifier;
import org.metaborg.meta.nabl2.util.collections.IProperties;
import org.metaborg.util.functions.Predicate2;

public interface ISolution {

    SolverConfig config();

    IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties();

    IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

    IEsopNameResolution<Scope, Label, Occurrence> nameResolution();

    IEsopNameResolution<Scope, Label, Occurrence> nameResolution(Predicate2<Scope, Label> isEdgeComplete);

    Optional<IEsopNameResolution.ResolutionCache<Scope, Label, Occurrence>> nameResolutionCache();

    IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties();

    Map<String, IVariantRelation.Immutable<ITerm>> relations();

    ISymbolicConstraints symbolic();

    IUnifier.Immutable unifier();

    IMessages.Immutable messages();

    java.util.Set<IConstraint> constraints();

    ISolution findAndLock() throws SolverException;

}