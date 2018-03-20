package mb.nabl2.solver;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate2;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.controlflow.terms.CFGNode;
import mb.nabl2.controlflow.terms.FlowSpecSolution;
import mb.nabl2.controlflow.terms.IFlowSpecSolution;
import mb.nabl2.relations.variants.IVariantRelation;
import mb.nabl2.relations.variants.VariantRelations;
import mb.nabl2.scopegraph.esop.IEsopNameResolution;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.scopegraph.esop.lazy.EsopNameResolution;
import mb.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.messages.Messages;
import mb.nabl2.stratego.TermIndex;
import mb.nabl2.symbolic.ISymbolicConstraints;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.collections.IProperties;
import mb.nabl2.util.collections.Properties;

@Value.Immutable(builder = true)
@Serial.Version(value = 1L)
public abstract class Solution implements ISolution {

    @Value.Parameter @Override public abstract SolverConfig config();

    @Value.Parameter @Override public abstract IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties();

    @Value.Parameter @Override public abstract IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

    @Override public IEsopNameResolution<Scope, Label, Occurrence> nameResolution() {
        return nameResolution((s, l) -> true);
    }

    @Override public IEsopNameResolution<Scope, Label, Occurrence>
            nameResolution(Predicate2<Scope, Label> isEdgeComplete) {
        final EsopNameResolution<Scope, Label, Occurrence> nr =
                EsopNameResolution.of(config().getResolutionParams(), scopeGraph(), isEdgeComplete);
        nameResolutionCache().ifPresent(nr::addAll);
        return nr;
    }

    @Value.Auxiliary @Override public abstract Optional<IEsopNameResolution.ResolutionCache<Scope, Label, Occurrence>>
            nameResolutionCache();

    @Value.Parameter @Override public abstract IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties();

    @Value.Parameter @Override public abstract Map<String, IVariantRelation.Immutable<ITerm>> relations();

    @Value.Parameter @Override public abstract IUnifier.Immutable unifier();

    @Value.Parameter @Override public abstract ISymbolicConstraints symbolic();
    
    @Value.Parameter @Override public abstract IFlowSpecSolution<CFGNode> flowSpecSolution();

    @Value.Parameter @Override public abstract IMessages.Immutable messages();

    @Value.Parameter @Override public abstract java.util.Set<IConstraint> constraints();

    public static ISolution of(SolverConfig config) {
        return ImmutableSolution.of(config, Properties.Immutable.of(), EsopScopeGraph.Immutable.of(),
                Properties.Immutable.of(), VariantRelations.immutableOf(config.getRelations()),
                PersistentUnifier.Immutable.of(), mb.nabl2.symbolic.SymbolicConstraints.of(),
                FlowSpecSolution.of(), Messages.Immutable.of(), Collections.emptySet());
    }

    @Override public ISolution findAndLock() {
        final IUnifier.Transient unifier = unifier().melt();
        // unifier.map(t -> t.withLocked(true)); // FIXME

        final Function1<ITerm, ITerm> findAndLock = t -> unifier.findRecursive(t).withLocked(true); // FIXME

        final IProperties.Transient<TermIndex, ITerm, ITerm> astProperties = astProperties().melt();
        astProperties.mapValues(findAndLock);

        final IProperties.Transient<Occurrence, ITerm, ITerm> declProperties = declProperties().melt();
        declProperties.mapValues(findAndLock);

        final ISymbolicConstraints symbolic = symbolic().map(findAndLock);

        return ImmutableSolution.builder().from(this).astProperties(astProperties.freeze())
                .declProperties(declProperties.freeze()).unifier(unifier.freeze()).symbolic(symbolic).build();
    }

}
