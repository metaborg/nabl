package mb.nabl2.terms.build;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableClassToInstanceMap;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.IListVar;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ListVar extends AbstractTermVar implements IListVar {

    @Override public <T> T match(IListTerm.Cases<T> cases) {
        return cases.caseVar(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IListTerm.CheckedCases<T, E> cases) throws E {
        return cases.caseVar(this);
    }

    @Override public abstract ListVar withAttachments(ImmutableClassToInstanceMap<Object> value);

}