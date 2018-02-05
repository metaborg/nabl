package org.metaborg.meta.nabl2.controlflow.terms;

import java.util.List;
import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.AbstractApplTerm;
import org.metaborg.meta.nabl2.terms.generic.TB;

import com.google.common.collect.ImmutableList;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CFGNode extends AbstractApplTerm implements ICFGNode, IApplTerm {

    private static final String OP = "CFGNode";

    // ICFGNode implementation

    @Value.Parameter @Override public abstract String getResource();

    @Value.Parameter @Override public abstract String getName();

    @Value.Parameter @Override public abstract ICFGNode.Kind getKind();

    // IApplTerm implementation

    @Override protected CFGNode check() {
        return this;
    }

    @Override public String getOp() {
        return OP;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return ImmutableList.of(TB.newString(getResource()), TB.newString(getName()), getKind());
    }

    public static IMatcher<CFGNode> matcher() {
        return M.appl3("CFGNode", M.stringValue(), M.stringValue(), 
                    M.appl().flatMap(appl -> Optional.ofNullable(ICFGNode.Kind.valueOf(appl.getOp()))),
                    (t, resource, name, kind) -> 
                        ImmutableCFGNode.of(resource, name, kind).withAttachments(t.getAttachments()));
    }

    // Object implementation

    @Override public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override public int hashCode() {
        return super.hashCode();
    }

    @Override public String toString() {
        return "##" + getName() + TermIndex.get(this).map(TermIndex::toString).orElse("");
    }

}