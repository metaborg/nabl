package org.metaborg.meta.nabl2.solver;

import java.util.Map;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.relations.variants.IVariantRelation;
import org.metaborg.meta.nabl2.relations.variants.VariantRelations;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.symbolic.ISymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.collections.IProperties;
import org.metaborg.meta.nabl2.util.collections.Properties;

@Value.Immutable(builder = true)
@Serial.Version(value = 1L)
public abstract class PublicSolution implements IPublicSolution {

    @Value.Parameter @Override public abstract SolverConfig config();

    @Value.Parameter @Override public abstract IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

    @Value.Parameter @Override public abstract IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties();

    @Value.Parameter @Override public abstract Map<IRelationName, IVariantRelation.Immutable<ITerm>> relations();

    @Value.Parameter @Override public abstract ISymbolicConstraints symbolic();

    public static IPublicSolution of(SolverConfig config) {
        return ImmutablePublicSolution.of(config, EsopScopeGraph.Immutable.of(), Properties.Immutable.of(),
                VariantRelations.immutableOf(config.getRelations()),
                org.metaborg.meta.nabl2.symbolic.SymbolicConstraints.of());
    }

}