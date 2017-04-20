package org.metaborg.meta.nabl2.terms;

import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;

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

}