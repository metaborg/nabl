package mb.statix.spec;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.MatchException;
import mb.nabl2.terms.matching.TermPattern;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.CannotUnifyException;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.Completeness;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.SolverResult;
import mb.statix.solver.State;
import mb.statix.solver.log.NullDebugContext;

@Value.Immutable
public abstract class ALambda {

    @Value.Parameter public abstract List<ITerm> params();

    public Set<ITermVar> paramVars() {
        return params().stream().flatMap(t -> t.getVars().stream()).collect(Collectors.toSet());
    }

    @Value.Parameter public abstract Set<ITermVar> bodyVars();

    @Value.Parameter public abstract List<IConstraint> body();

    @Value.Lazy public Optional<Boolean> isAlways(Spec spec) throws InterruptedException {
        State state = State.of(spec);
        List<ITerm> args = Lists.newArrayList();
        for(@SuppressWarnings("unused") ITerm param : params()) {
            final Tuple2<ITermVar, State> stateAndVar = state.freshVar("arg");
            args.add(stateAndVar._1());
            state = stateAndVar._2();
        }
        Tuple2<State, Lambda> stateAndInst;
        try {
            stateAndInst = apply(args, state);
        } catch(MatchException | CannotUnifyException e) {
            throw new IllegalStateException();
        }
        state = stateAndInst._1();
        final Lambda inst = stateAndInst._2();
        try {
            Optional<SolverResult> solverResult =
                    Solver.entails(state, inst.body(), new Completeness(), inst.bodyVars(), new NullDebugContext());
            if(solverResult.isPresent()) {
                return Optional.of(true);
            } else {
                return Optional.of(false);
            }
        } catch(Delay d) {
            return Optional.empty();
        }
    }

    public Lambda apply(ISubstitution.Immutable subst) {
        final ISubstitution.Immutable bodySubst = subst.removeAll(paramVars()).removeAll(bodyVars());
        final List<IConstraint> newBody = body().stream().map(c -> c.apply(bodySubst)).collect(Collectors.toList());
        return Lambda.of(params(), bodyVars(), newBody);
    }

    public Tuple2<State, Lambda> apply(List<ITerm> args, State state) throws MatchException, CannotUnifyException {
        final ISubstitution.Transient subst = new TermPattern(params()).match(state.unifier()::areEqual, args).melt();
        State newState = state;
        final ImmutableSet.Builder<ITermVar> freshBodyVars = ImmutableSet.builder();
        for(ITermVar var : bodyVars()) {
            final Tuple2<ITermVar, State> vs = newState.freshVar(var.getName());
            subst.put(var, vs._1());
            freshBodyVars.add(vs._1());
            newState = vs._2();
        }
        final ISubstitution.Immutable isubst = subst.freeze();
        final Set<IConstraint> newBody = body().stream().map(c -> c.apply(isubst)).collect(Collectors.toSet());
        final Lambda newRule = Lambda.of(args, freshBodyVars.build(), newBody);
        return ImmutableTuple2.of(newState, newRule);
    }

    public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append(unifier.toString(params()));
        if(!body().isEmpty()) {
            sb.append(" :- ");
            if(!bodyVars().isEmpty()) {
                sb.append("{").append(unifier.toString(bodyVars())).append("} ");
            }
            sb.append(IConstraint.toString(body(), unifier));
        }
        sb.append(" }");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}