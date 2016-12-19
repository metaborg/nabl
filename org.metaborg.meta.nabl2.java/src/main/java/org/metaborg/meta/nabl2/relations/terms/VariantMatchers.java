package org.metaborg.meta.nabl2.relations.terms;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;
import org.metaborg.meta.nabl2.relations.IVariance;
import org.metaborg.meta.nabl2.relations.IVariantMatcher;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.metaborg.meta.nabl2.util.Iterables3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

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

    public static IVariantMatcher<ITerm> covariantList() {
        return new ListVariant(ImmutableCovariant.of(ImmutableRelationName.of(Optional.empty())));
    }

    private static class ListVariant implements IVariantMatcher<ITerm> {

        private final IVariance variance;

        public ListVariant(IVariance variance) {
            this.variance = variance;
        }

        @Override public Optional<Iterable<IVariantMatcher.Arg<ITerm>>> match(ITerm t) {
            return M.list(list -> {
                List<IVariantMatcher.Arg<ITerm>> args = Lists.newArrayList();
                for (ITerm arg : list) {
                    args.add(ImmutableArg.of(variance, arg));
                }
                return (Iterable<IVariantMatcher.Arg<ITerm>>) args;
            }).match(t);
        }

        @Override public ITerm build(Iterable<? extends ITerm> ts) {
            return GenericTerms.newList(ts);
        }

    }

    private static class OpVariant implements IVariantMatcher<ITerm> {

        private final String op;
        private final ImmutableList<IVariance> variances;

        public OpVariant(String op, Iterable<IVariance> variances) {
            this.op = op;
            this.variances = ImmutableList.copyOf(variances);
        }

        @Override public Optional<Iterable<IVariantMatcher.Arg<ITerm>>> match(ITerm t) {
            return M.appl(op, appl -> Iterables3.<IVariance, ITerm, IVariantMatcher.Arg<ITerm>> zipStrict(variances,
                    appl.getArgs(), (v, a) -> {
                        return (IVariantMatcher.Arg<ITerm>) ImmutableArg.of(v, a);
                    })).match(t).flatMap(o -> o);
        }

        @Override public ITerm build(Iterable<? extends ITerm> ts) {
            return GenericTerms.newAppl(op, ts);
        }

    }

    @Value.Immutable
    static abstract class Arg implements IVariantMatcher.Arg<ITerm> {

        @Value.Parameter @Override public abstract IVariance getVariance();

        @Value.Parameter @Override public abstract ITerm getValue();

    }

}