package org.metaborg.meta.nabl2.terms;

import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface IListTerm extends ITerm, Iterable<ITerm> {

    <T> T match(Cases<T> cases);

    interface Cases<T> {

        T caseCons(IConsTerm cons);

        T caseNil(INilTerm nil);

        T caseVar(ITermVar var);

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> extends CheckedFunction1<IListTerm,T,E> {

        T caseCons(IConsTerm cons) throws E;

        T caseNil(INilTerm nil) throws E;

        T caseVar(ITermVar var) throws E;

    }

    IListTerm withAttachments(ImmutableClassToInstanceMap<Object> value);

}