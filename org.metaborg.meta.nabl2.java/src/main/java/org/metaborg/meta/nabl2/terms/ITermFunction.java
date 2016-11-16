package org.metaborg.meta.nabl2.terms;

import java.util.function.Function;

public interface ITermFunction<T> {

    T apply(IApplTerm applTerm);

    T apply(ITupleTerm tuple);

    T apply(IConsTerm cons);

    T apply(INilTerm nil);

    T apply(IStringTerm string);

    T apply(IIntTerm i);

    T apply(ITermVar var);

    static <T> ITermFunction<T> of(Function<? super IApplTerm,T> applFun, Function<? super ITupleTerm,T> tupleFun,
            Function<? super IConsTerm,T> consFun, Function<? super INilTerm,T> nilFun,
            Function<? super IStringTerm,T> stringFun, Function<? super IIntTerm,T> intFun,
            Function<? super ITermVar,T> varFun) {
        return new ITermFunction<T>() {

            @Override public T apply(IApplTerm applTerm) {
                return applFun.apply(applTerm);
            }

            @Override public T apply(ITupleTerm tupleTerm) {
                return tupleFun.apply(tupleTerm);
            }

            @Override public T apply(IConsTerm consTerm) {
                return consFun.apply(consTerm);
            }

            @Override public T apply(INilTerm nilTerm) {
                return nilFun.apply(nilTerm);
            }

            @Override public T apply(IStringTerm stringTerm) {
                return stringFun.apply(stringTerm);
            }

            @Override public T apply(IIntTerm intTerm) {
                return intFun.apply(intTerm);
            }

            @Override public T apply(ITermVar termVar) {
                return varFun.apply(termVar);
            }

        };
    }

}