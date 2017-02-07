package org.metaborg.meta.nabl2.terms;

import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.functions.Function2;

public class ListTerms {

    public static <T> IListTerm.Cases<T> cases(
        // @formatter:off
        Function1<? super IConsTerm,T> onCons,
        Function1<? super INilTerm,T> onNil,
        Function1<? super ITermVar,T> onVar
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

        };
    }

    public static <T> IListTerm.Cases<T> casesFix(
        // @formatter:off
        Function2<IListTerm.Cases<T>, ? super IConsTerm, ? extends T> onCons,
        Function2<IListTerm.Cases<T>, ? super INilTerm, ? extends T> onNil,
        Function2<IListTerm.Cases<T>, ? super ITermVar, ? extends T> onVar
        // @formatter:on
    ) {
        return new IListTerm.Cases<T>() {

            @Override public T caseCons(IConsTerm cons) {
                return onCons.apply(this, cons);
            }

            @Override public T caseNil(INilTerm nil) {
                return onNil.apply(this, nil);
            }

            @Override public T caseVar(ITermVar var) {
                return onVar.apply(this, var);
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