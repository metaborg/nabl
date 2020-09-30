package mb.nabl2.solver;

import java.util.Collections;
import java.util.Map;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.relations.variants.IVariantRelation;
import mb.nabl2.relations.variants.VariantRelations;
import mb.nabl2.scopegraph.esop.IEsopNameResolution;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.OccurrenceIndex;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.messages.Messages;
import mb.nabl2.symbolic.ISymbolicConstraints;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.collections.IProperties;
import mb.nabl2.util.collections.Properties;

@Value.Immutable(builder = true)
@Serial.Version(value = 42L)
public abstract class ASolution implements ISolution {

    @Value.Parameter @Override public abstract SolverConfig config();

    @Value.Parameter @Override public abstract IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties();

    @Value.Parameter @Override public abstract IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

    @Value.Lazy @Override public Multimap<OccurrenceIndex, Occurrence> astDecls() {
        final ImmutableMultimap.Builder<OccurrenceIndex, Occurrence> astDecls = ImmutableMultimap.builder();
        scopeGraph().getAllDecls().forEach(o -> {
            astDecls.put(o.getIndex(), o);
        });
        return astDecls.build();
    }

    @Value.Lazy @Override public Multimap<OccurrenceIndex, Occurrence> astRefs() {
        final ImmutableMultimap.Builder<OccurrenceIndex, Occurrence> astRefs = ImmutableMultimap.builder();
        scopeGraph().getAllRefs().forEach(o -> {
            astRefs.put(o.getIndex(), o);
        });
        return astRefs.build();
    }

    @Override public IEsopNameResolution<Scope, Label, Occurrence> nameResolution() {
        final IEsopNameResolution<Scope, Label, Occurrence> nr = IEsopNameResolution.of(config().getResolutionParams(),
                scopeGraph(), (s, l) -> true, nameResolutionCache());
        return nr;
    }

    @Value.Default @Override public IEsopNameResolution.IResolutionCache<Scope, Label, Occurrence>
            nameResolutionCache() {
        return IEsopNameResolution.IResolutionCache.empty();
    }

    @Value.Parameter @Override public abstract IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties();

    @Value.Parameter @Override public abstract Map<String, IVariantRelation.Immutable<ITerm>> relations();

    @Value.Parameter @Override public abstract IUnifier.Immutable unifier();

    @Value.Parameter @Override public abstract ISymbolicConstraints symbolic();

    @Value.Parameter @Override public abstract IMessages.Immutable messages();

    @Value.Parameter @Override public abstract java.util.Set<IConstraint> constraints();

    public static ISolution of(SolverConfig config) {
        return Solution.of(config, Properties.Immutable.of(), EsopScopeGraph.Immutable.of(), Properties.Immutable.of(),
                VariantRelations.immutableOf(config.getRelations()), Unifiers.Immutable.of(),
                mb.nabl2.symbolic.SymbolicConstraints.of(), Messages.Immutable.of(), Collections.emptySet());
    }

}