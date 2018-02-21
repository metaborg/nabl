package org.metaborg.meta.nabl2.controlflow.terms;

import static org.metaborg.meta.nabl2.terms.build.TermBuild.B;
import static org.metaborg.meta.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.build.AbstractApplTerm;
import org.metaborg.meta.nabl2.terms.matching.TermMatch.IMatcher;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap.Builder;
import com.google.common.collect.ImmutableList;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CFGNode extends AbstractApplTerm implements ICFGNode, IApplTerm {

    private static final String OP = "CFGNode";

    // ICFGNode implementation

    @Value.Parameter public abstract TermIndex getIndex();

    // Auxiliary except when the node is artificial
    @Value.Parameter @Value.Auxiliary @Nullable @Override public abstract String getName();

    @Value.Parameter @Override public abstract ICFGNode.Kind getKind();

    // IApplTerm implementation

    @Override protected CFGNode check() {
        Builder<Object> newAttachments = ImmutableClassToInstanceMap.builder();
        newAttachments.putAll(this.getAttachments());
        newAttachments.put(TermIndex.class, getIndex());
        return this.withAttachments(newAttachments.build());
    }

    @Override public abstract CFGNode withAttachments(ImmutableClassToInstanceMap<Object> value);

    @Override public String getOp() {
        return OP;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return ImmutableList.of(getIndex(), B.newString(getName()), getKind());
    }

    public static IMatcher<CFGNode> matcher() {
        return M.appl3("CFGNode", TermIndex.matcher(), M.stringValue(), 
                    M.appl().flatMap(appl -> Optional.ofNullable(ICFGNode.Kind.valueOf(appl.getOp()))),
                    (t, index, name, kind) -> 
                        ImmutableCFGNode.of(index, name, kind).withAttachments(t.getAttachments()));
    }

    // Object implementation

    @Override public boolean equals(@Nullable Object another) {
      if (this == another) return true;
      return another instanceof ImmutableCFGNode
          && equalTo((ImmutableCFGNode) another);
    }

    private boolean equalTo(ImmutableCFGNode another) {
      return getIndex().equals(another.getIndex())
          && getKind().equals(another.getKind())
          && (getKind() != Kind.Artificial || Objects.equals(getName(), another.getName()));
    }

    @Override public int hashCode() {
        int h = 5381;
        h += (h << 5) + getIndex().hashCode();
        h += (h << 5) + getKind().hashCode();
        if (getKind() == Kind.Artificial) {
            h += (h << 5) + getName().hashCode();
        }
        return h;
    }

    @Override public String toString() {
        return "##" + getName() + this.getIndex().toString();
    }

    public static CFGNode normal(TermIndex index) {
        return normal(index, null);
    }

    public static CFGNode normal(TermIndex index, @Nullable String name) {
        return ImmutableCFGNode.of(index, name, Kind.Normal);
    }

    public static CFGNode start(TermIndex index) {
        return start(index, null);
    }

    public static CFGNode start(TermIndex index, @Nullable String name) {
        return ImmutableCFGNode.of(index, name, Kind.Start);
    }

    public static CFGNode end(TermIndex index) {
        return end(index, null);
    }

    public static CFGNode end(TermIndex index, @Nullable String name) {
        return ImmutableCFGNode.of(index, name, Kind.End);
    }

    public static CFGNode artificial(TermIndex index, @Nonnull String name) {
        return ImmutableCFGNode.of(index, Objects.requireNonNull(name), Kind.Artificial);
    }

}