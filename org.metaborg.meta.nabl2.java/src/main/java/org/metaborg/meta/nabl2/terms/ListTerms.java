package org.metaborg.meta.nabl2.terms;

import java.util.function.Function;

import org.metaborg.meta.nabl2.functions.CheckedFunction1;

public class ListTerms {

    public static <T> IListTerm.Cases<T> cases(
        // @formatter:off
        Function<? super IConsTerm,T> onCons,
        Function<? super INilTerm,T> onNil,
        Function<? super ITermVar,T> onVar
        // @formatter:on
    ) {
        return new IListTerm.Cases<T>() {

            @Override public T caseCons(IConsTerm cons) {
                return onCons.apply(cons);
            }

            @Override public T caseNil(INilTerm nil) {
                return onNil.apply(nil);
            }

            @Override public T caseVar(ITermVar var) {
                return onVar.apply(var);
            }

            @Override public T apply(IListTerm list) {
                return list.match(this);
            }

        };
    }

    public static <T, E extends Throwable> IListTerm.CheckedCases<T,E> checkedCases(
        // @formatter:off
        CheckedFunction1<? super IConsTerm,T,E> onCons,
        CheckedFunction1<? super INilTerm,T,E> onNil,
        CheckedFunction1<? super ITermVar,T,E> onVar
        // @formatter:on
    ) {
        return new IListTerm.CheckedCases<T,E>() {

            @Override public T caseCons(IConsTerm cons) throws E {
                return onCons.apply(cons);
            }

            @Override public T caseNil(INilTerm nil) throws E {
                return onNil.apply(nil);
            }

            @Override public T caseVar(ITermVar var) throws E {
                return onVar.apply(var);
            }

            @Override public T apply(IListTerm list) throws E {
                return list.matchOrThrow(this);
            }

        };
    }

}