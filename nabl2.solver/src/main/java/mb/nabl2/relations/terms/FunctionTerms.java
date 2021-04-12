package mb.nabl2.relations.terms;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.tuple.Tuple2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import mb.nabl2.relations.terms.FunctionTerms.Eval;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.matching.TermPattern;
import mb.nabl2.terms.substitution.ISubstitution;

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
            return Tuple2.of(name.getName(), new Eval(cases));
        });
    }

    private static IMatcher<Tuple2<Pattern, ITerm>> functionCase() {
        return M.tuple2(M.term(), M.term(), (t, pattern, term) -> {
            if(!pattern.getVars().containsAll(term.getVars())) {
                throw new IllegalStateException("Function case is not closed.");
            }
            return Tuple2.of(TermPattern.P.fromTerm(pattern), term);
        });
    }

    public static class Eval implements PartialFunction1<ITerm, ITerm>, Serializable {
        private static final long serialVersionUID = 42L;

        private final List<Tuple2<Pattern, ITerm>> cases;

        private Eval(List<Tuple2<Pattern, ITerm>> cases) {
            this.cases = ImmutableList.copyOf(cases);
        }

        @Override public Optional<ITerm> apply(ITerm term) {
            if(!term.isGround()) {
                throw new IllegalStateException("Term argument must be ground.");
            }
            for(Tuple2<Pattern, ITerm> c : cases) {
                final Pattern pattern = c._1();
                final ISubstitution.Immutable matchResult;
                if((matchResult = pattern.match(term).orElse(null)) != null) {
                    final ITerm result = matchResult.apply(c._2());
                    return Optional.of(result);
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