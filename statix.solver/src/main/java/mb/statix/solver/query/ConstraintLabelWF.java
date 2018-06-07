package mb.statix.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.constraint.CUser;

public class ConstraintLabelWF implements LabelWF<ITerm> {

    private final String constraint;
    private final State state;
    private final Completeness completeness;
    private final IDebugContext debug;

    private final List<ITerm> labels;

    public ConstraintLabelWF(String constraint, State state, Completeness completeness, IDebugContext debug) {
        this(constraint, state, completeness, debug, ImmutableList.of());
    }

    private ConstraintLabelWF(String constraint, State state, Completeness completeness, IDebugContext debug,
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
        final IConstraint C = new CUser(constraint, ImmutableList.of(term));
        final Config config = Config.of(state, ImmutableList.of(C), completeness);
        return Solver.entails(config, debug).orElseThrow(() -> new ResolutionException());
    }

    @Override public boolean empty() throws ResolutionException, InterruptedException {
        final Tuple2<ITermVar, State> varAndState = state.freshVar("lbls");
        final ITermVar var = varAndState._1();
        final ITerm term = B.newListTail(labels, var);
        final IConstraint C = new CUser(constraint, ImmutableList.of(term));
        final Config config = Config.of(varAndState._2(), ImmutableList.of(C), completeness);
        return Solver.entails(config, Iterables2.singleton(var), debug).orElseThrow(() -> new ResolutionException());
    }

}