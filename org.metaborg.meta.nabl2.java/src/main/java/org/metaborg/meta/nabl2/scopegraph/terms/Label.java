package org.metaborg.meta.nabl2.scopegraph.terms;

import java.util.List;

import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.AbstractApplTerm;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.metaborg.meta.nabl2.util.functions.Function1;

import com.google.common.collect.ImmutableList;

@Value.Immutable
public abstract class Label extends AbstractApplTerm implements ILabel, IApplTerm {

    private static final String OP = "Label";

    // ILabel implementation

    @Value.Parameter @Override public abstract String getName();

    // IApplTerm implementation

    @Value.Lazy @Override public String getOp() {
        return OP;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return ImmutableList.of((ITerm) GenericTerms.newString(getName()));
    }

    public static IMatcher<Label> matcher() {
        return matcher(l -> l);
    }

    public static <R> IMatcher<R> matcher(Function1<Label,R> f) {
        return M.cases(
            // @formatter:off
            M.appl0("D", (t) -> f.apply(ImmutableLabel.of("D"))),
            M.appl0("P", (t) -> f.apply(ImmutableLabel.of("P"))),
            M.appl0("I", (t) -> f.apply(ImmutableLabel.of("I"))),
            M.appl1(OP, M.stringValue(), (t,l) -> f.apply(ImmutableLabel.of(l)))
            // @formatter:on
        );
    }

    // Object implementation

    @Override public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override public String toString() {
        return super.toString();
    }

}