package mb.nabl2.terms.build;

import static org.metaborg.util.unit.Unit.unit;

import java.util.List;
import java.util.Objects;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IConsList;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ListTerms;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class ConsList extends AbstractApplTerm implements IConsList {

    @Override protected ConsList check() {
        return this;
    }

    @Value.Parameter @Override public abstract ITerm getHead();

    @Value.Parameter @Override public abstract IListTerm getTail();

    @Override public String getOp() {
        return ListTerms.CONS_OP;
    }

    @Override public List<ITerm> getArgs() {
        return ImmutableList.of(getHead(), getTail());
    }

    @Override public <T> T match(IListTerm.Cases<T> cases) {
        return cases.caseCons(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IListTerm.CheckedCases<T, E> cases) throws E {
        return cases.caseCons(this);
    }

    @Override public int hashCode() {
        return Objects.hash(getHead(), getTail());
    }

    @Override public boolean equals(Object other) {
        if(other == null) {
            return false;
        }
        if(!(other instanceof IConsList)) {
            return false;
        }
        IConsList that = (IConsList) other;
        if(!getHead().equals(that.getHead())) {
            return false;
        }
        if(!getTail().equals(that.getTail())) {
            return false;
        }
        return true;
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