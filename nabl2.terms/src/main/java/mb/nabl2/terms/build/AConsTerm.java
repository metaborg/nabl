package mb.nabl2.terms.build;

import static org.metaborg.util.unit.Unit.unit;

import java.util.Objects;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.IConsTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;

@Value.Immutable(lazyhash = false)
@Serial.Version(value = 42L)
abstract class AConsTerm extends AbstractTerm implements IConsTerm {

    @Value.Parameter @Override public abstract ITerm getHead();

    @Value.Parameter @Override public abstract IListTerm getTail();

    @Value.Lazy @Override public int getMinSize() {
        return 1 + getTail().getMinSize();
    }

    @Value.Lazy @Override public boolean isGround() {
        return getHead().isGround() && getTail().isGround();
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        if(isGround()) {
            return CapsuleUtil.immutableSet();
        }
        final Set.Transient<ITermVar> vars = CapsuleUtil.transientSet();
        visitVars(vars::__insert);
        return vars.freeze();
    }

    @Override public void visitVars(Action1<ITermVar> onVar) {
        if(isGround()) {
            return;
        }
        getHead().visitVars(onVar);
        getTail().visitVars(onVar);
    }

    @Override public <T> T match(ITerm.Cases<T> cases) {
        return cases.caseList(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(ITerm.CheckedCases<T, E> cases) throws E {
        return cases.caseList(this);
    }

    @Override public <T> T match(IListTerm.Cases<T> cases) {
        return cases.caseCons(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IListTerm.CheckedCases<T, E> cases) throws E {
        return cases.caseCons(this);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(getHead(), getTail());
            hashCode = result;
        }
        return result;
    }

    @Override public boolean equals(Object other) {
        if(this == other)
            return true;
        if(!(other instanceof IConsTerm))
            return false;
        IConsTerm that = (IConsTerm) other;
        if(this.hashCode() != that.hashCode())
            return false;
        // @formatter:off
        return Objects.equals(this.getHead(), that.getHead())
            && Objects.equals(this.getTail(), that.getTail());
        // @formatter:on
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(getHead());
        getTail().match(ListTerms.casesFix(
        // @formatter:off
            (f,cons) -> {
                sb.append(",");
                sb.append(cons.getHead());
                return cons.getTail().match(f);
            },
            (f,nil) -> unit,
            (f,var) -> {
                sb.append("|");
                sb.append(var);
                return unit;
            }
            // @formatter:on
        ));
        sb.append("]");
        return sb.toString();
    }

}
