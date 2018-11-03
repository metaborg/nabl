package mb.statix.spec;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.MatchException;
import mb.nabl2.terms.matching.TermPattern;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.CannotUnifyException;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.TermFormatter;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;

public class Rule {

    private final String name;
    private final List<ITerm> patterns;
    private final Set<ITermVar> bodyVars;
    private final List<IConstraint> body;

    public Rule(String name, Iterable<? extends ITerm> patterns, Iterable<ITermVar> bodyVars,
            Iterable<IConstraint> body) {
        this.name = name;
        this.patterns = ImmutableList.copyOf(patterns);
        this.bodyVars = ImmutableSet.copyOf(bodyVars);
        this.body = ImmutableList.copyOf(body);
    }

    public String getName() {
        return name;
    }

    public List<ITerm> getParams() {
        return patterns;
    }

    public Set<ITermVar> getBodyVars() {
        return bodyVars;
    }

    public List<IConstraint> getBody() {
        return body;
    }

    public Tuple2<State, Rule> apply(List<ITerm> args, State state) throws MatchException {
        final ISubstitution.Transient subst = new TermPattern(patterns).match(state.unifier()::areEqual, args).melt();
        State newState = state;
        // body vars
        final ImmutableSet.Builder<ITermVar> freshBodyVars = ImmutableSet.builder();
        for(ITermVar var : bodyVars) {
            final Tuple2<ITermVar, State> vs = newState.freshVar(var.getName());
            subst.put(var, vs._1());
            freshBodyVars.add(vs._1());
            newState = vs._2();
        }
        final ISubstitution.Immutable isubst = subst.freeze();
        final Set<IConstraint> newBody = body.stream().map(c -> c.apply(isubst)).collect(Collectors.toSet());
        final Rule newRule = new Rule(name, args, freshBodyVars.build(), newBody);
        return ImmutableTuple2.of(newState, newRule);
    }

    public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("(");
        sb.append(termToString.apply(patterns));
        sb.append(")");
        if(!body.isEmpty()) {
            sb.append(" :- ");
            if(!bodyVars.isEmpty()) {
                sb.append("{").append(termToString.apply(bodyVars)).append("} ");
            }
            sb.append(IConstraint.toString(body, termToString));
        }
        sb.append(".");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    public static java.util.Comparator<Rule> leftRightPatternOrdering = new LeftRightPatternOrder();

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    private static class LeftRightPatternOrder implements Comparator<Rule> {

        public int compare(Rule r1, Rule r2) {
            TermPattern p1 = new TermPattern(r1.patterns);
            TermPattern p2 = new TermPattern(r2.patterns);
            return TermPattern.leftRightOrdering.compare(p1, p2);
        }

    }

}
