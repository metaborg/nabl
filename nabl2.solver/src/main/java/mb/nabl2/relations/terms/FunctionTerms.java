package mb.nabl2.relations.terms;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.functions.PartialFunction1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import mb.nabl2.relations.terms.FunctionName.NamedFunction;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.MatchException;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

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

        private final List<Tuple2<ITerm, ITerm>> cases;

        private Eval(List<Tuple2<ITerm, ITerm>> cases) {
            this.cases = ImmutableList.copyOf(cases);
        }

        @Override public Optional<ITerm> apply(ITerm term) {
            if(!term.isGround()) {
                throw new IllegalStateException("Term argument must be ground.");
            }
            for(Tuple2<ITerm, ITerm> c : cases) {
                final ISubstitution.Immutable subst;
                try {
                    subst = PersistentSubstitution.Immutable.of().match(c._1(), term);
                    final ITerm result = subst.apply(c._2());
                    return Optional.of(result);
                } catch(MatchException e) {
                }
            }
            return Optional.empty();
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + cases.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            final Eval other = (Eval) obj;
            if(!cases.equals(other.cases))
                return false;
            return true;
        }

    }

}