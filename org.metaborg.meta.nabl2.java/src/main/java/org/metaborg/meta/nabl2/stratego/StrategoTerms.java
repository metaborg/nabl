package org.metaborg.meta.nabl2.stratego;

import java.util.function.Function;
import java.util.function.Supplier;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;

public class StrategoTerms {

    public static <T> T match(IStrategoTerm term, Cases<T> visitor) {
        switch (term.getTermType()) {
        case IStrategoTerm.APPL:
            return visitor.caseAppl((IStrategoAppl) term);
        case IStrategoTerm.LIST:
            return visitor.caseList((IStrategoList) term);
        case IStrategoTerm.TUPLE:
            return visitor.caseTuple((IStrategoTuple) term);
        case IStrategoTerm.INT:
            return visitor.caseInt((IStrategoInt) term);
        case IStrategoTerm.STRING:
            return visitor.caseString((IStrategoString) term);
        default:
            throw new IllegalArgumentException("type of " + term + "is not supported.");
        }
    }

    public static <T> Cases<T> cases(
        // @formatter:off
        Function<? super IStrategoAppl,T> onAppl,
        Function<? super IStrategoTuple,T> onTuple,
        Function<? super IStrategoList,T> onList,
        Function<? super IStrategoInt,T> onInt,
        Function<? super IStrategoString,T> onString
        // @formatter:on
    ) {
        return new Cases<T>() {

            @Override public T caseAppl(IStrategoAppl term) {
                return onAppl.apply(term);
            }

            @Override public T caseList(IStrategoList term) {
                return onList.apply(term);
            }

            @Override public T caseTuple(IStrategoTuple term) {
                return onTuple.apply(term);
            }

            @Override public T caseInt(IStrategoInt term) {
                return onInt.apply(term);
            }

            @Override public T caseString(IStrategoString term) {
                return onString.apply(term);
            }

            @Override public T apply(IStrategoTerm term) {
                return match(term, this);
            }

        };
    }

    public interface Cases<T> extends Function<IStrategoTerm,T> {

        T caseAppl(IStrategoAppl term);

        T caseTuple(IStrategoTuple term);

        T caseList(IStrategoList term);

        T caseInt(IStrategoInt term);

        T caseString(IStrategoString term);

    }

    public static <T> CaseBuilder<T> cases() {
        return new CaseBuilder<T>();
    }

    public static class CaseBuilder<T> {

        private Function<? super IStrategoAppl,T> onAppl;
        private Function<? super IStrategoTuple,T> onTuple;
        private Function<? super IStrategoList,T> onList;
        private Function<? super IStrategoInt,T> onInt;
        private Function<? super IStrategoString,T> onString;


        public CaseBuilder<T> appl(Function<? super IStrategoAppl,T> onAppl) {
            this.onAppl = onAppl;
            return this;
        }

        public CaseBuilder<T> tuple(Function<? super IStrategoTuple,T> onTuple) {
            this.onTuple = onTuple;
            return this;
        }

        public CaseBuilder<T> list(Function<? super IStrategoList,T> onList) {
            this.onList = onList;
            return this;
        }

        public CaseBuilder<T> integer(Function<? super IStrategoInt,T> onInt) {
            this.onInt = onInt;
            return this;
        }

        public CaseBuilder<T> string(Function<? super IStrategoString,T> onString) {
            this.onString = onString;
            return this;
        }

        public Cases<T> otherwise(Supplier<T> otherwise) {
            return new Cases<T>() {

                @Override public T caseAppl(IStrategoAppl term) {
                    return onAppl != null ? onAppl.apply(term) : otherwise.get();
                }

                @Override public T caseTuple(IStrategoTuple term) {
                    return onTuple != null ? onTuple.apply(term) : otherwise.get();
                }

                @Override public T caseList(IStrategoList term) {
                    return onList != null ? onList.apply(term) : otherwise.get();
                }

                @Override public T caseInt(IStrategoInt term) {
                    return onInt != null ? onInt.apply(term) : otherwise.get();
                }

                @Override public T caseString(IStrategoString term) {
                    return onString != null ? onString.apply(term) : otherwise.get();
                }

                @Override public T apply(IStrategoTerm term) {
                    return match(term, this);
                }

            };

        }

    }

}