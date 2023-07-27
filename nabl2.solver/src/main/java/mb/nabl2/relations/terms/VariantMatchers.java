package mb.nabl2.relations.terms;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.optionals.Optionals;

import mb.nabl2.relations.variants.IVariance;
import mb.nabl2.relations.variants.IVariantMatcher;
import mb.nabl2.relations.variants.Variances;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

public class VariantMatchers {

    public static IMatcher<IVariantMatcher<ITerm>> matcher() {
        return M.cases(
        // @formatter:off
                M.appl1("ListVariant", Variances.matcher(), (t, v) -> new ListVariant(v)),
                M.appl2("OpVariant", M.stringValue(), M.listElems(Variances.matcher()),
                        (t, op, vs) -> new OpVariant(op, vs)),
                M.appl1("TupleVariant", M.listElems(Variances.matcher()), (t, vs) -> new OpVariant(Terms.TUPLE_OP, vs))
        // @formatter:on
        );
    }

    private static class ListVariant implements IVariantMatcher<ITerm>, Serializable {
        private static final long serialVersionUID = 42L;

        private final IVariance variance;

        public ListVariant(IVariance variance) {
            this.variance = variance;
        }

        @Override public Optional<List<IArg<ITerm>>> match(ITerm t) {
            return M.listElems(M.term(), (l, list) -> {
                List<IVariantMatcher.IArg<ITerm>> args = new ArrayList<>();
                for(ITerm arg : list) {
                    args.add(Arg.of(variance, arg));
                }
                return (List<IVariantMatcher.IArg<ITerm>>) args;
            }).match(t);
        }

        @Override public ITerm build(Collection<? extends ITerm> ts) {
            return B.newList(ts);
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + variance.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            final ListVariant other = (ListVariant) obj;
            if(!variance.equals(other.variance))
                return false;
            return true;
        }

    }

    private static class OpVariant implements IVariantMatcher<ITerm>, Serializable {

        private static final long serialVersionUID = 42L;

        private final String op;
        private final ImList.Immutable<IVariance> variances;

        public OpVariant(String op, Iterable<IVariance> variances) {
            this.op = op;
            this.variances = ImList.Immutable.copyOf(variances);
        }

        @Override public Optional<List<IArg<ITerm>>> match(ITerm t) {
            return IMatcher.flatten(M.appl(op, appl -> {
                return Optionals.when(variances.size() == appl.getArity()).map(eq -> {
                    final List<IArg<ITerm>> result = new ArrayList<>();
                    Iterables2.zip(variances, appl.getArgs(), Arg::of).forEach(arg -> result.add(arg));
                    return result;
                });
            })).match(t);
        }

        @Override public ITerm build(Collection<? extends ITerm> ts) {
            return B.newAppl(op, ts);
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + op.hashCode();
            result = prime * result + variances.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            final OpVariant other = (OpVariant) obj;
            if(!op.equals(other.op))
                return false;
            if(!variances.equals(other.variances))
                return false;
            return true;
        }

    }

    @Value.Immutable
    static abstract class AArg implements IVariantMatcher.IArg<ITerm>, Serializable {

        private static final long serialVersionUID = 42L;

        @Value.Parameter @Override public abstract IVariance getVariance();

        @Value.Parameter @Override public abstract ITerm getValue();

    }

}