package mb.statix.random.node;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.core.MetaborgException;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Rule;

public class ExpandPredicate extends SearchNode<Tuple2<SearchState, CUser>, SearchState> {

    public ExpandPredicate(Random rnd) {
        super(rnd);
    }

    private CUser predicate;
    private Set<Rule> rules;

    @Override protected void doInit() {
        this.predicate = input._2();
        this.rules = new HashSet<>(input._1().state().spec().rules().get(predicate.name()));
    }

    @Override protected Optional<SearchState> doNext() throws MetaborgException {
        if(rules.isEmpty()) {
            return Optional.empty();
        }
        // FIXME The current method can cause capture if a solver-generated variable
        //       has the same name as a pattern variable in the rule. The arguments
        //       contain solver variables, but they are substituted under an exists
        //       with the literal rule pattern variables. To do this correctly,
        //       we must rename pattern variables that might clash.
        final Rule rule = pick(rules);
        final HashMultimap<ITerm, ITerm> eqMap = HashMultimap.create();
        for(@SuppressWarnings("unused") Object dummy : Iterables2.zip(rule.params(), predicate.args(), (p, a) -> {
            // FIXME Pattern::asTerm does not work if wildcards appear in the pattern
            Tuple2<ITerm, Multimap<ITermVar, ITerm>> termAndEqs = p.asTerm();
            eqMap.putAll(termAndEqs._2());
            eqMap.put(termAndEqs._1(), a);
            return null;
        })) {
        }
        ;
        final List<CEqual> eqs = eqMap.entries().stream().map(eq -> new CEqual(eq.getKey(), eq.getValue(), predicate))
                .collect(Collectors.toList());
        final IConstraint constraint =
                new CExists(rule.paramVars(), new CConj(Constraints.conjoin(eqs), rule.body()), predicate);
        final SearchState state = input._1();
        final SearchState newState = state.update(state.state(), Iterables2.cons(constraint, state.constraints()));
        return Optional.of(newState);
    }

    @Override public String toString() {
        return "expand-goal-rule";
    }

}