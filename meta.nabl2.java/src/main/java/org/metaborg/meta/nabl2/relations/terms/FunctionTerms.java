package org.metaborg.meta.nabl2.relations.terms;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.relations.terms.FunctionName.NamedFunction;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.functions.PartialFunction1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class FunctionTerms {

    public static IMatcher<Map<String, PartialFunction1<ITerm, ITerm>>> functions() {
        return M.listElems(function(), (l, funDefs) -> {
            ImmutableMap.Builder<String, PartialFunction1<ITerm, ITerm>> functions = ImmutableMap.builder();
            for(Tuple2<String, Eval> funDef : funDefs) {
                functions.put(funDef._1(), funDef._2());
            }
            return functions.build();
        });
    }

    private static IMatcher<Tuple2<String, Eval>> function() {
        return M.tuple2(NamedFunction.matcher(), M.listElems(functionCase()), (t, name, cases) -> {
            return ImmutableTuple2.of(name.getName(), new Eval(cases));
        });
    }

    private static IMatcher<Tuple2<ITerm, ITerm>> functionCase() {
        return M.tuple2(M.term(), M.term(), (t, t1, t2) -> {
            if(!t1.getVars().containsAll(t2.getVars())) {
                throw new IllegalStateException("Function case is not closed.");
            }
            return ImmutableTuple2.of(t1, t2);
        });
    }

    public static class Eval implements PartialFunction1<ITerm, ITerm>, Serializable {
        private static final long serialVersionUID = 42L;

        private final ImmutableList<Tuple2<ITerm, ITerm>> cases;

        private Eval(ImmutableList<Tuple2<ITerm, ITerm>> cases) {
            this.cases = cases;
        }

        @Override public Optional<ITerm> apply(ITerm term) {
            if(!term.isGround()) {
                throw new IllegalStateException("Term argument must be ground.");
            }
            for(Tuple2<ITerm, ITerm> c : cases) {
                Unifier.Transient unifier = Unifier.Transient.of();
                try {
                    unifier.unify(c._1(), term);
                    ITerm result = unifier.find(c._2());
                    return Optional.of(result);
                } catch(UnificationException e) {
                }
            }
            return Optional.empty();

        }

    }

}