package mb.statix.random;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Rule;

public abstract class SearchNode<I, O> {

    private final AtomicBoolean init = new AtomicBoolean(false);

    protected final Random rnd;

    public SearchNode(Random rnd) {
        this.rnd = rnd;
    }

    protected I input;

    public void init(I i) {
        init.set(true);
        this.input = i;
        doInit();
    }

    protected abstract void doInit();

    public Optional<O> next() throws MetaborgException, InterruptedException {
        if(!init.get()) {
            throw new IllegalStateException();
        }
        return doNext();
    }

    protected abstract Optional<O> doNext() throws MetaborgException, InterruptedException;

    protected <E> E pick(Set<E> set) {
        final int index = rnd.nextInt(set.size());
        final Iterator<E> iter = set.iterator();
        for(int i = 0; i < index; i++) {
            iter.next();
        }
        final E element = iter.next();
        iter.remove();
        return element;
    }

    protected IConstraint apply(Rule rule, List<ITerm> args, IConstraint cause) {
        // FIXME The current method can cause capture if a solver-generated variable
        //       has the same name as a pattern variable in the rule. The arguments
        //       contain solver variables, but they are substituted under an exists
        //       with the literal rule pattern variables. To do this correctly,
        //       we must rename pattern variables that might clash.
        final HashMultimap<ITerm, ITerm> eqMap = HashMultimap.create();
        for(@SuppressWarnings("unused") Object dummy : Iterables2.zip(rule.params(), args, (p, a) -> {
            // FIXME Pattern::asTerm does not work if wildcards appear in the pattern
            Tuple2<ITerm, Multimap<ITermVar, ITerm>> termAndEqs = p.asTerm();
            eqMap.putAll(termAndEqs._2());
            eqMap.put(termAndEqs._1(), a);
            return null;
        })) {
        }
        ;
        final List<CEqual> eqs = eqMap.entries().stream().map(eq -> new CEqual(eq.getKey(), eq.getValue(), cause))
                .collect(Collectors.toList());
        final IConstraint constraint =
                new CExists(rule.paramVars(), new CConj(Constraints.conjoin(eqs), rule.body()), cause);
        return constraint;
    }

}