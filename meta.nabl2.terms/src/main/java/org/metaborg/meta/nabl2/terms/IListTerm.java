package org.metaborg.meta.nabl2.terms;

import java.util.Optional;

import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.util.functions.CheckedFunction1;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface IListTerm extends ITerm {

    <T> T match(Cases<T> cases);

    interface Cases<T> {

        T caseCons(IConsTerm cons);

        T caseNil(INilTerm nil);

        T caseVar(ITermVar var);

        default T caseLock(IListTerm list) {
            return list.match(this);
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> extends CheckedFunction1<IListTerm, T, E> {

        T caseCons(IConsTerm cons) throws E;

        T caseNil(INilTerm nil) throws E;

        T caseVar(ITermVar var) throws E;

        default T caseLock(IListTerm list) throws E {
            return list.matchOrThrow(this);
        }

    }

    IListTerm withAttachments(ImmutableClassToInstanceMap<Object> value);

    @Override IListTerm withLocked(boolean locked);

    public static IMatcher<IListTerm> matcher() {
        return term -> {
            if(term instanceof IListTerm) {
                return Optional.of((IListTerm) term);
            } else {
                return Optional.empty();
            }
        };
    }

}