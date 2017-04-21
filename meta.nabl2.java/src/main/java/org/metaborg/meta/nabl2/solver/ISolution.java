package org.metaborg.meta.nabl2.solver;

import java.util.Set;

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

public interface ISolution {

    /**
     * @return The configuration used for this solution
     */
    SolverConfig getConfig();

    IScopeGraph<Scope, Label, Occurrence> getScopeGraph();

    INameResolution<Scope, Label, Occurrence> getNameResolution();

    IProperties<Occurrence> getDeclProperties();

    IProperties<TermIndex> getAstProperties();

    IRelations<ITerm> getRelations();

    ISymbolicConstraints getSymbolic();

    IUnifier getUnifier();

    IMessages getMessages();

    Set<IConstraint> getUnsolvedConstraints();

}