package org.metaborg.meta.nabl2.solver;

import java.util.Collections;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.symbolic.ISymbolicConstraints;
import org.metaborg.meta.nabl2.symbolic.SymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.collections.IProperties;
import org.metaborg.meta.nabl2.util.collections.Properties;

@Value.Immutable(builder = true, prehash = true)
@Serial.Version(value = 1L)
public abstract class Solution implements ISolution {

    @Value.Parameter @Override public abstract SolverConfig config();

    @Value.Default @Override public IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties() {
        return Properties.Immutable.of();
    }

    @Value.Default @Override public IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph() {
        return EsopScopeGraph.Immutable.of();
    }

    @Value.Lazy @Override public IEsopNameResolution<Scope, Label, Occurrence> nameResolution() {
        return scopeGraph().resolve(config().getResolutionParams(), (s, l) -> true);
    }

    @Value.Default @Override public IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties() {
        return Properties.Immutable.of();
    }

    @Value.Default @Override public IRelations.Immutable<ITerm> relations() {
        return config().getRelations();

    }

    @Value.Default @Override public IUnifier.Immutable unifier() {
        return Unifier.Immutable.of();
    }

    @Value.Default @Override public ISymbolicConstraints symbolic() {
        return SymbolicConstraints.of();

    }

    @Value.Default @Override public IMessages.Immutable messages() {
        return Messages.Immutable.of();
    }

    @Value.Default @Override public java.util.Set<IConstraint> constraints() {
        return Collections.emptySet();
    }

    public static Solution of(SolverConfig config, Iterable<? extends IConstraint> constraints) {
        return ImmutableSolution.builder().config(config).constraints(constraints).build();
    }

    public static Solution of(SolverConfig config) {
        return ImmutableSolution.builder().config(config).build();
    }

}