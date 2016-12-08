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

import com.google.common.collect.ImmutableList;

@Value.Immutable
public abstract class Label extends AbstractApplTerm implements ILabel, IApplTerm {

    private static final String OP = "Label";

    // ILabel implementation

    @Value.Parameter @Override public abstract String getName();

    // IApplTerm implementation

    @Override public String getOp() {
        return OP;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return ImmutableList.of(GenericTerms.newString(getName()));
    }

    public static IMatcher<Label> matcher() {
        return M.cases(
            // @formatter:off
            M.appl0("P", (t) -> ImmutableLabel.of("P").setAttachments(t.getAttachments())),
            M.appl0("I", (t) -> ImmutableLabel.of("I").setAttachments(t.getAttachments())),
            M.appl1(OP, M.stringValue(), (t,l) -> ImmutableLabel.of(l).setAttachments(t.getAttachments()))
            // @formatter:on
        );
    }

    // Object implementation

    @Override public String toString() {
        return super.toString();
    }

}