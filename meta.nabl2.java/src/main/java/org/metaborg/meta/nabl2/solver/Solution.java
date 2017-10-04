package org.metaborg.meta.nabl2.solver;

import java.util.Collections;
import java.util.Map;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.controlflow.terms.CFGNode;
import org.metaborg.meta.nabl2.relations.variants.IVariantRelation;
import org.metaborg.meta.nabl2.relations.variants.VariantRelations;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.symbolic.ISymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.collections.IProperties;
import org.metaborg.meta.nabl2.util.collections.Properties;
import org.metaborg.util.functions.Function1;

import meta.flowspec.nabl2.controlflow.IControlFlowGraph;
import meta.flowspec.nabl2.controlflow.impl.ControlFlowGraph;

@Value.Immutable(builder = true)
@Serial.Version(value = 1L)
public abstract class Solution implements ISolution {

    @Value.Parameter @Override public abstract SolverConfig config();

    @Value.Parameter @Override public abstract IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties();

    @Value.Parameter @Override public abstract IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

    @Value.Parameter @Override public abstract IEsopNameResolution.Immutable<Scope, Label, Occurrence> nameResolution();

    @Value.Parameter @Override public abstract IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties();

    @Value.Parameter @Override public abstract Map<String, IVariantRelation.Immutable<ITerm>> relations();

    @Value.Parameter @Override public abstract IUnifier.Immutable unifier();

    @Value.Parameter @Override public abstract ISymbolicConstraints symbolic();
    
    @Value.Parameter @Override public abstract IControlFlowGraph<CFGNode> controlFlowGraph();

    @Value.Parameter @Override public abstract IMessages.Immutable messages();

    @Value.Parameter @Override public abstract java.util.Set<IConstraint> constraints();

    public static ISolution of(SolverConfig config) {
        return ImmutableSolution.of(config, Properties.Immutable.of(), EsopScopeGraph.Immutable.of(),
                EsopNameResolution.Immutable.of(config.getResolutionParams()), Properties.Immutable.of(),
                VariantRelations.immutableOf(config.getRelations()), Unifier.Immutable.of(),
                org.metaborg.meta.nabl2.symbolic.SymbolicConstraints.of(), ControlFlowGraph.of(), Messages.Immutable.of(),
                Collections.emptySet());
    }

    @Override public ISolution findAndLock() throws SolverException {
        try {
            final IUnifier.Transient unifier = unifier().melt();
            unifier.map(t -> t.withLocked(true));

            final Function1<ITerm, ITerm> findAndLock = t -> unifier.find(t).withLocked(true);

            final IProperties.Transient<TermIndex, ITerm, ITerm> astProperties = astProperties().melt();
            astProperties.mapValues(findAndLock);

            final IProperties.Transient<Occurrence, ITerm, ITerm> declProperties = declProperties().melt();
            declProperties.mapValues(findAndLock);

            final ISymbolicConstraints symbolic = symbolic().map(findAndLock);

            return ImmutableSolution.builder().from(this).astProperties(astProperties.freeze())
                    .declProperties(declProperties.freeze()).unifier(unifier.freeze()).symbolic(symbolic).build();
        } catch(UnificationException ex) {
            throw new SolverException(ex);
        }
    }

}