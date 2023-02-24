package mb.nabl2.solver;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.relations.variants.IVariantRelation;
import mb.nabl2.relations.variants.VariantRelations;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.messages.Messages;
import mb.nabl2.symbolic.ISymbolicConstraints;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.collections.IProperties;
import mb.nabl2.util.collections.Properties;
import mb.scopegraph.pepm16.esop15.IEsopNameResolution;
import mb.scopegraph.pepm16.esop15.IEsopScopeGraph;
import mb.scopegraph.pepm16.esop15.reference.EsopScopeGraph;
import mb.scopegraph.pepm16.terms.Label;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.OccurrenceIndex;
import mb.scopegraph.pepm16.terms.Scope;

@Value.Immutable(builder = true)
@Serial.Version(value = 42L)
public abstract class ASolution implements ISolution {

    @Value.Parameter @Override public abstract SolverConfig config();

    @Value.Parameter @Override public abstract IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties();

    @Value.Parameter @Override public abstract IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

    @Value.Lazy @Override public SetMultimap.Immutable<OccurrenceIndex, Occurrence> astDecls() {
        final SetMultimap.Transient<OccurrenceIndex, Occurrence> astDecls = SetMultimap.Transient.of();
        scopeGraph().getAllDecls().forEach(o -> {
            astDecls.__put(o.getIndex(), o);
        });
        return astDecls.freeze();
    }

    @Value.Lazy @Override public SetMultimap.Immutable<OccurrenceIndex, Occurrence> astRefs() {
        final SetMultimap.Transient<OccurrenceIndex, Occurrence> astRefs = SetMultimap.Transient.of();
        scopeGraph().getAllRefs().forEach(o -> {
            astRefs.__put(o.getIndex(), o);
        });
        return astRefs.freeze();
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

    @Value.Parameter @Override public abstract Map.Immutable<String, IVariantRelation.Immutable<ITerm>> relations();

    @Value.Parameter @Override public abstract IUnifier.Immutable unifier();

    @Value.Parameter @Override public abstract ISymbolicConstraints symbolic();

    @Value.Parameter @Override public abstract IMessages.Immutable messages();

    @Value.Parameter @Override public abstract Set.Immutable<IConstraint> constraints();

    public static ISolution of(SolverConfig config) {
        return Solution.of(config, Properties.Immutable.of(), EsopScopeGraph.Immutable.of(), Properties.Immutable.of(),
                VariantRelations.immutableOf(config.getRelations()), Unifiers.Immutable.of(),
                mb.nabl2.symbolic.SymbolicConstraints.of(), Messages.Immutable.of(), Set.Immutable.of());
    }

}