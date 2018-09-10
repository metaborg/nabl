package mb.nabl2.controlflow.terms;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap.Builder;
import com.google.common.collect.ImmutableList;

import mb.nabl2.stratego.TermIndex;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Value.Check @Override protected CFGNode check() {
        if (TermIndex.get(this).map(ti -> ti.equals(getIndex())).orElse(false)) {
            return this;
        }
        Builder<Object> newAttachments = ImmutableClassToInstanceMap.builder();
        this.getAttachments().entrySet().stream().filter(e -> e.getKey() != TermIndex.class).forEach(e -> {
            newAttachments.put((Class) e.getKey(), e.getValue());
        });
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
                    M.appl().<ICFGNode.Kind>flatMap(appl -> Optionals.ofThrowing(() -> ICFGNode.Kind.valueOf(appl.getOp()))),
                    (t, index, name, kind) -> 
                        ImmutableCFGNode.of(index, name, kind).withAttachments(t.getAttachments()));
    }

    @Override public abstract boolean equals(Object other);

    @Override public abstract int hashCode();

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

    public static CFGNode entry(TermIndex index) {
        return entry(index, null);
    }

    public static CFGNode entry(TermIndex index, @Nullable String name) {
        return ImmutableCFGNode.of(index, name, Kind.Entry);
    }

    public static CFGNode exit(TermIndex index) {
        return exit(index, null);
    }

    public static CFGNode exit(TermIndex index, @Nullable String name) {
        return ImmutableCFGNode.of(index, name, Kind.Exit);
    }

}