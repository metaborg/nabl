package org.metaborg.meta.nabl2.terms;

public abstract class ATermFunction<T> implements ITermFunction<T> {

    @Override public T apply(IApplTerm appl) {
        return defaultApply(appl);
    }

    @Override public T apply(ITupleTerm tuple) {
        return defaultApply(tuple);
    }

    @Override public T apply(IConsTerm cons) {
        return defaultApply(cons);
    }

    @Override public T apply(INilTerm nil) {
        return defaultApply(nil);
    }

    @Override public T apply(IStringTerm string) {
        return defaultApply(string);
    }

    @Override public T apply(IIntTerm integer) {
        return defaultApply(integer);
    }

    @Override public T apply(ITermVar var) {
        return defaultApply(var);
    }

    protected abstract T defaultApply(ITerm term);

}