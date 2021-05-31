package mb.nabl2.solver;

import java.util.Map;

import com.google.common.collect.Multimap;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.relations.variants.IVariantRelation;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.messages.Messages;
import mb.nabl2.symbolic.ISymbolicConstraints;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.collections.IProperties;
import mb.scopegraph.pepm16.esop15.IEsopNameResolution;
import mb.scopegraph.pepm16.esop15.IEsopScopeGraph;
import mb.scopegraph.pepm16.terms.Label;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.OccurrenceIndex;
import mb.scopegraph.pepm16.terms.Scope;

public interface ISolution {

    SolverConfig config();

    IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties();

    ISolution withAstProperties(IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties);

    IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

    Multimap<OccurrenceIndex, Occurrence> astRefs();

    Multimap<OccurrenceIndex, Occurrence> astDecls();

    ISolution withScopeGraph(IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph);

    IEsopNameResolution<Scope, Label, Occurrence> nameResolution();

    IEsopNameResolution.IResolutionCache<Scope, Label, Occurrence> nameResolutionCache();

    IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties();

    ISolution withDeclProperties(IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties);

    Map<String, IVariantRelation.Immutable<ITerm>> relations();

    ISolution withRelations(Map<String, ? extends IVariantRelation.Immutable<ITerm>> relations);

    ISymbolicConstraints symbolic();

    ISolution withSymbolic(ISymbolicConstraints symbolic);

    IUnifier.Immutable unifier();

    ISolution withUnifier(IUnifier.Immutable unifier);

    IMessages.Immutable messages();

    ISolution withMessages(IMessages.Immutable messages);

    default IMessages.Immutable messagesAndUnsolvedErrors() {
        IMessages.Transient messages = messages().melt();
        messages.addAll(Messages.unsolvedErrors(constraints()));
        return messages.freeze();
    }

    java.util.Set<IConstraint> constraints();

    ISolution withConstraints(Iterable<? extends IConstraint> constraints);

}