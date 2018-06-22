package mb.statix.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.MatchException;
import mb.nabl2.terms.unification.UnificationException;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.Config;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.spec.Lambda;

public class ConstraintLabelWF implements LabelWF<ITerm> {

    private final Lambda constraint;
    private final State state;
    private final Completeness completeness;
    private final IDebugContext debug;

    private final List<ITerm> labels;

    public ConstraintLabelWF(Lambda constraint, State state, Completeness completeness, IDebugContext debug) {
        this(constraint, state, completeness, debug, ImmutableList.of());
    }

    private ConstraintLabelWF(Lambda constraint, State state, Completeness completeness, IDebugContext debug,
            Iterable<ITerm> labels) {
        this.constraint = constraint;
        this.state = state;
        this.completeness = completeness;
        this.debug = debug;
        this.labels = ImmutableList.copyOf(labels);
    }

    public LabelWF<ITerm> step(ITerm l) {
        final List<ITerm> labels = ImmutableList.<ITerm>builder().addAll(this.labels).add(l).build();
        return new ConstraintLabelWF(constraint, state, completeness, debug, labels);
    }

    @Override public boolean wf() throws ResolutionException, InterruptedException {
        final ITerm term = B.newList(labels);
        debug.info("Check {} well-formed", state.unifier().toString(term));
        try {
            final Tuple2<State, Lambda> result = constraint.apply(ImmutableList.of(term), state);
            final Config config = Config.of(result._1(), result._2().getBody(), completeness);
            if(Solver.entails(config, result._2().getBodyVars(), debug.subContext())
                    .orElseThrow(() -> new ResolutionException("Label well-formedness check delayed"))) {
                debug.info("Well-formed {}", state.unifier().toString(term));
                return true;
            } else {
                debug.info("Not well-formed {}", state.unifier().toString(term));
                return false;
            }
        } catch(MatchException | UnificationException ex) {
            return false;
        }
    }

    @Override public boolean empty() throws ResolutionException, InterruptedException {
        final Tuple2<ITermVar, State> varAndState = state.freshVar("lbls");
        final ITermVar var = varAndState._1();
        final ITerm term = B.newListTail(labels, var);
        debug.info("Check {} empty", state.unifier().toString(term));
        try {
            final Tuple2<State, Lambda> result = constraint.apply(ImmutableList.of(term), varAndState._2());
            final Config config = Config.of(result._1(), result._2().getBody(), completeness);
            if(!Solver.entails(config, Iterables2.singleton(var), debug.subContext()).orElse(true)) {
                debug.info("Empty {}", state.unifier().toString(term));
                return true;
            } else {
                debug.info("Non-empty {}", state.unifier().toString(term));
                return false;
            }
        } catch(MatchException | UnificationException ex) {
            return false;
        }
    }

}