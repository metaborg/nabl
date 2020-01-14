package mb.nabl2.terms.build;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.INilList;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ListTerms;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class NilList extends AbstractApplTerm implements INilList {

    @Override protected NilList check() {
        return this;
    }

    @Override public String getOp() {
        return ListTerms.NIL_OP;
    }

    @Override public List<ITerm> getArgs() {
        return ImmutableList.of();
    }

    @Override public <T> T match(IListTerm.Cases<T> cases) {
        return cases.caseNil(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IListTerm.CheckedCases<T, E> cases) throws E {
        return cases.caseNil(this);
    }

    @Override public int hashCode() {
        return 1;
    }

    @Override public boolean equals(Object other) {
        if(other == null) {
            return false;
        }
        if(!(other instanceof INilList)) {
            return false;
        }
        return true;
    }

    @Override public String toString() {
        return "[]";
    }

}