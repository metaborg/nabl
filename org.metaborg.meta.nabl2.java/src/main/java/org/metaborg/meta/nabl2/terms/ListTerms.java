package org.metaborg.meta.nabl2.terms;

import java.util.function.Function;

public class ListTerms {

    public static <T> IListTerm.Cases<T> cases(
        // @formatter:off
        Function<? super IConsTerm,T> onCons,
        Function<? super INilTerm,T> onNil
        // @formatter:on
    ) {
        return new IListTerm.Cases<T>() {

            @Override public T caseCons(IConsTerm cons) {
                return onCons.apply(cons);
            }

            @Override public T caseNil(INilTerm nil) {
                return onNil.apply(nil);
            }

            @Override public T apply(IListTerm list) {
                return list.match(this);
            }

        };
    }

}