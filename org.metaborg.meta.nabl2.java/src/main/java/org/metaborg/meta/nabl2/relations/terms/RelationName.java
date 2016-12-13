package org.metaborg.meta.nabl2.relations.terms;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.INamespace;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.AbstractApplTerm;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;

import com.google.common.collect.ImmutableList;

@Value.Immutable
public abstract class RelationName extends AbstractApplTerm implements INamespace, IApplTerm {

    private static final String OP1 = "Relation";
    private static final String OP0 = "DefaultRelation";

    // INamespace implementation

    @Value.Parameter @Override public abstract Optional<String> getName();

    // IApplTerm implementation

    @Value.Lazy @Override public String getOp() {
        return getName().map(name -> OP1).orElse(OP0);
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return getName()
            // @formatter:off
            .map(name -> ImmutableList.of((ITerm)GenericTerms.newString(name)))
            .orElseGet(() -> ImmutableList.of());
            // @formatter:on
    }

    public static IMatcher<RelationName> matcher() {
        return M.cases(
            // @formatter:off
            M.appl0(OP0, (t) -> ImmutableRelationName.of(Optional.empty()).setAttachments(t.getAttachments())),
            M.appl1(OP1, M.stringValue(), (t, ns) -> ImmutableRelationName.of(Optional.of(ns)).setAttachments(t.getAttachments()))
            // @formatter:on
        );
    }

    @Override public String toString() {
        return super.toString();
    }

}