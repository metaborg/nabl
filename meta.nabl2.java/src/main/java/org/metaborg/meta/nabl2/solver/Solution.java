package org.metaborg.meta.nabl2.solver;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;

@Value.Immutable
@Serial.Version(value = 1L)
public abstract class Solution implements ISolution {

    @Value.Parameter @Override public abstract SolverConfig getConfig();

    @Value.Parameter @Override public abstract IProperties<TermIndex> getAstProperties();

    @Value.Parameter @Override public abstract IScopeGraph<Scope, Label, Occurrence> getScopeGraph();

    @Value.Parameter @Override public abstract INameResolution<Scope, Label, Occurrence> getNameResolution();

    @Value.Parameter @Override public abstract IProperties<Occurrence> getDeclProperties();

    @Value.Parameter @Override public abstract IRelations<ITerm> getRelations();

    @Value.Parameter @Override public abstract IUnifier getUnifier();

    @Value.Parameter @Override public abstract ISymbolicConstraints getSymbolic();

    @Value.Parameter public abstract IMessages getMessages();

    @Value.Parameter @Override public abstract Set<IConstraint> getUnsolvedConstraints();

}