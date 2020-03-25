package mb.nabl2.terms.build;

import static org.metaborg.util.unit.Unit.unit;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;

import mb.nabl2.terms.IConsList;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
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

    @Value.Lazy @Override public int getMinSize() {
        return 1 + getTail().getMinSize();
    }

    @Value.Lazy @Override public boolean isGround() {
        return getHead().isGround() && getTail().isGround();
    }

    @Value.Lazy @Override public ImmutableMultiset<ITermVar> getVars() {
        final ImmutableMultiset.Builder<ITermVar> vars = ImmutableMultiset.builder();
        vars.addAll(getHead().getVars());
        vars.addAll(getTail().getVars());
        return vars.build();
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
        return super.hashCode();
    }

    @Override public boolean equals(Object other) {
        return super.equals(other);
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
