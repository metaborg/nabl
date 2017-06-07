package org.metaborg.meta.nabl2.solver;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.relations.IRelations;
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
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.collections.IProperties;
import org.metaborg.meta.nabl2.util.collections.Properties;
import org.metaborg.meta.nabl2.util.functions.Function1;

@Value.Immutable(builder = true)
@Serial.Version(value = 1L)
public abstract class Solution implements ISolution {

    @Value.Parameter @Override public abstract SolverConfig config();

    @Value.Parameter @Override public abstract IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties();

    @Value.Parameter @Override public abstract IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

    @Value.Parameter @Override public abstract IEsopNameResolution.Immutable<Scope, Label, Occurrence> nameResolution();

    @Value.Parameter @Override public abstract IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties();

    @Value.Parameter @Override public abstract IRelations.Immutable<ITerm> relations();

    @Value.Parameter @Override public abstract IUnifier.Immutable unifier();

    @Value.Parameter @Override public abstract ISymbolicConstraints symbolic();

    @Value.Parameter @Override public abstract IMessages.Immutable messages();

    @Value.Parameter @Override public abstract java.util.Set<IConstraint> constraints();

    @Override public ISolution findAndLock() {
        final Function1<ITerm, ITerm> findAndLock = t -> unifier().find(t).withLocked(true);
        final IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties =
                Properties.map(astProperties(), findAndLock).freeze();
        final IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties =
                Properties.map(declProperties(), findAndLock).freeze();
        final IUnifier.Immutable unifier = Unifier.findAndLock(unifier());
        final ISymbolicConstraints symbolic = symbolic().map(findAndLock);
        return ImmutableSolution.builder().from(this).astProperties(astProperties).declProperties(declProperties)
                .unifier(unifier).symbolic(symbolic).build();
    }

}