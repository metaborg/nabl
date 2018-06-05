package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;

import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.NameResolution;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;
import mb.statix.spec.Type;

public class CResolveQuery implements IConstraint {

    private final ITerm relation;
    private final Object filter;
    private final Object min;
    private final ITerm scopeTerm;
    private final ITerm resultTerm;

    public CResolveQuery(ITerm relation, Object filter, Object min, ITerm scopeTerm, ITerm resultTerm) {
        this.relation = relation;
        this.filter = filter;
        this.min = min;
        this.scopeTerm = scopeTerm;
        this.resultTerm = resultTerm;
    }

    @Override public IConstraint apply(Function1<ITerm, ITerm> map) {
        return new CResolveQuery(relation, filter, min, map.apply(scopeTerm), map.apply(resultTerm));
    }

    @Override public Optional<Config> solve(State state, IDebugContext debug) {
        final Type type = state.spec().relations().get(relation);
        if(type == null) {
            debug.error("Ignoring query for unknown relation {}", relation);
            return Optional.of(Config.builder().state(state).addConstraints(new CFalse()).build());
        }

        final IUnifier.Immutable unifier = state.unifier();
        if(!unifier.isGround(scopeTerm)) {
            return Optional.empty();
        }
        final Scope scope = Scope.matcher().match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + scopeTerm));

        NameResolution.Builder<ITerm, ITerm, ITerm, ITerm> nameResolution = NameResolution.builder();
        if(relation.equals(B.EMPTY_TUPLE)) {
            // query scopes
            return Optional.empty();
        } else {
            nameResolution.withEdgeComplete((s, l) -> false);
            nameResolution.withDataComplete((s, r) -> false);
        }
        try {
            final Set<IResolutionPath<ITerm, ITerm, ITerm, ITerm>> paths =
                    nameResolution.build(state.scopeGraph(), relation).resolve(scope);
            final List<IApplTerm> pathTerms = paths.stream()
                    .map(p -> B.newTuple(B.newBlob(p.getPath()), p.getDeclaration())).collect(Collectors.toList());
            final IConstraint C = new CEqual(B.newList(pathTerms), resultTerm);
            return Optional.of(Config.builder().state(state).addConstraints(C).build());
        } catch(ResolutionException e) {
            return Optional.empty();
        }
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("query ");
        sb.append(relation);
        sb.append(" filter ");
        sb.append(filter);
        sb.append(" min ");
        sb.append(min);
        sb.append(" in ");
        sb.append(unifier.toString(scopeTerm));
        sb.append(" |-> ");
        sb.append(unifier.toString(resultTerm));
        return sb.toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("query ");
        sb.append(relation);
        sb.append(" filter ");
        sb.append(filter);
        sb.append(" min ");
        sb.append(min);
        sb.append(" in ");
        sb.append(scopeTerm);
        sb.append(" |-> ");
        sb.append(resultTerm);
        return sb.toString();
    }

}