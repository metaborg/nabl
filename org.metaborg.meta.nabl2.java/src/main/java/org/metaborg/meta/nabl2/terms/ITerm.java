package org.metaborg.meta.nabl2.terms;

import java.util.function.Function;

import org.metaborg.meta.nabl2.functions.CheckedFunction1;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface ITerm {

    boolean isGround();

    ImmutableClassToInstanceMap<IAnnotation> getAnnotations();

    <T> T match(Cases<T> cases);

    interface Cases<T> extends Function<ITerm,T> {

        T caseAppl(IApplTerm appl);

        T caseList(IListTerm cons);

        T caseString(IStringTerm string);

        T caseInt(IIntTerm integer);

        T caseVar(ITermVar var);

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> extends CheckedFunction1<ITerm,T,E> {

        T caseAppl(IApplTerm appl) throws E;

        T caseList(IListTerm cons) throws E;

        T caseString(IStringTerm string) throws E;

        T caseInt(IIntTerm integer) throws E;

        T caseVar(ITermVar var) throws E;

    }

}