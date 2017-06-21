package org.metaborg.meta.nabl2.solver;

import java.util.Map;

import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.relations.variants.IVariantRelation;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.symbolic.ISymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.collections.IProperties;

public interface IPublicSolution {

    SolverConfig config();

    IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

    IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties();

    Map<IRelationName, IVariantRelation.Immutable<ITerm>> relations();

    ISymbolicConstraints symbolic();

}