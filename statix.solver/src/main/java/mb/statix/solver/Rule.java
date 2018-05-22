package mb.statix.solver;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.MatchException;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public class Rule {

    private final String name;
    private final List<ITermVar> params;
    private final Set<ITermVar> guardVars;
    private final Set<IConstraint> guardConstraints;
    private final Set<ITermVar> bodyVars;
    private final Set<IConstraint> bodyConstraints;

    public Rule(String name, Iterable<ITermVar> params, Iterable<ITermVar> guardVars, Iterable<IConstraint> guardConstraints,
            Iterable<ITermVar> bodyVars, Iterable<IConstraint> bodyConstraints) {
        this.name = name;
        this.params = ImmutableList.copyOf(params);
        this.guardVars = ImmutableSet.copyOf(guardVars);
        this.guardConstraints = ImmutableSet.copyOf(guardConstraints);
        this.bodyVars = ImmutableSet.copyOf(bodyVars);
        this.bodyConstraints = ImmutableSet.copyOf(bodyConstraints);
    }

    public String getName() {
        return name;
    }
    
    public Tuple2<Config, Set<IConstraint>> apply(List<ITerm> args, State state) throws MatchException {
        IUnifier.Transient unifier = PersistentUnifier.Transient.of();
        for(int i = 0; i < params.size(); i++) {
            unifier.match(params.get(i), args.get(i));
        }
        int fresh = state.fresh();
        for(ITermVar var : guardVars) {
            unifier.match(var, B.newVar("", var.getName() + "-" + (++fresh)));
        }
        for(ITermVar var : bodyVars) {
            unifier.match(var, B.newVar("", var.getName() + "-" + (++fresh)));
        }
        final State newState = state.withFresh(fresh);
        final Set<IConstraint> newGuard =
                guardConstraints.stream().map(c -> c.apply(unifier::findRecursive)).collect(Collectors.toSet());
        final Set<IConstraint> newBody =
                bodyConstraints.stream().map(c -> c.apply(unifier::findRecursive)).collect(Collectors.toSet());
        return ImmutableTuple2.of(Config.of(newState, newGuard), newBody);
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("(");
        sb.append(params);
        sb.append(")");
        sb.append(" | ");
        sb.append(guardConstraints);
        sb.append(" :- ");
        sb.append(bodyConstraints);
        sb.append(" .");
        return sb.toString();
    }

}