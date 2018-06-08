package mb.statix.solver.constraint;

import java.util.Optional;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.solver.Completeness;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Result;
import mb.statix.solver.State;
import mb.statix.spoofax.StatixTerms;

public class CPathLt implements IConstraint {

    private final IRelation.Immutable<ITerm> lt;
    private final ITerm label1Term;
    private final ITerm label2Term;

    public CPathLt(IRelation.Immutable<ITerm> lt, ITerm l1, ITerm l2) {
        this.lt = lt;
        this.label1Term = l1;
        this.label2Term = l2;
    }

    @Override public IConstraint apply(Function1<ITerm, ITerm> map) {
        return new CPathLt(lt, map.apply(label1Term), map.apply(label2Term));
    }

    @Override public Optional<Result> solve(State state, Completeness completeness, IDebugContext debug) {
        final IUnifier unifier = state.unifier();
        if(!(unifier.isGround(label1Term) && unifier.isGround(label2Term))) {
            return Optional.empty();
        }
        final ITerm label1 = StatixTerms.label().match(label1Term, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected label, got " + label1Term));
        final ITerm label2 = StatixTerms.label().match(label2Term, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected label, got " + label2Term));
        if(lt.contains(label1, label2)) {
            return Optional.of(Result.of(state, ImmutableSet.of()));
        } else {
            return Optional.of(Result.of(state, ImmutableSet.of(new CFalse())));
        }


    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("pathLt[");
        sb.append(lt);
        sb.append("](");
        sb.append(unifier.toString(label1Term));
        sb.append(",");
        sb.append(unifier.toString(label2Term));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("pathLt[");
        sb.append(lt);
        sb.append("](");
        sb.append(label1Term);
        sb.append(",");
        sb.append(label2Term);
        sb.append(")");
        return sb.toString();
    }

}