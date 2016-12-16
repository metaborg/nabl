package org.metaborg.meta.nabl2.relations;

import org.metaborg.meta.nabl2.util.functions.Function0;
import org.metaborg.meta.nabl2.util.functions.Function1;

public interface IVariance {

    <T> T match(Cases<T> cases);

    public static <T> Cases<T> cases(Function0<T> onInvariant, Function1<IRelationName,T> onConvariant,
            Function1<IRelationName,T> onContravariant) {
        return new Cases<T>() {

            @Override public T caseInvariant() {
                return onInvariant.apply();
            }

            @Override public T caseCovariant(IRelationName name) {
                return onConvariant.apply(name);
            }

            @Override public T caseContravariant(IRelationName name) {
                return onContravariant.apply(name);
            }

        };
    }

    interface Cases<T> {

        T caseInvariant();

        T caseCovariant(IRelationName name);

        T caseContravariant(IRelationName name);

    }

}