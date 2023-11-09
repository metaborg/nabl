package mb.nabl2.terms;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;

public class Terms {

    public static final String TUPLE_OP = "";
    public static final String VAR_OP = "nabl2.Var";

    // SAFE

    public static <T> ITerm.Cases<T> cases(
    // @formatter:off
        Function1<? super IApplTerm, ? extends T> onAppl,
        Function1<? super IListTerm, ? extends T> onList,
        Function1<? super IStringTerm, ? extends T> onString,
        Function1<? super IIntTerm, ? extends T> onInt,
        Function1<? super IBlobTerm, ? extends T> onBlob,
        Function1<? super ITermVar, ? extends T> onVar
        // @formatter:on
    ) {
        return new ITerm.Cases<T>() {

            @Override public T caseAppl(IApplTerm appl) {
                return onAppl.apply(appl);
            }

            @Override public T caseList(IListTerm list) {
                return onList.apply(list);
            }

            @Override public T caseString(IStringTerm string) {
                return onString.apply(string);
            }

            @Override public T caseInt(IIntTerm integer) {
                return onInt.apply(integer);
            }

            @Override public T caseBlob(IBlobTerm blob) {
                return onBlob.apply(blob);
            }

            @Override public T caseVar(ITermVar var) {
                return onVar.apply(var);
            }

        };
    }

    public static <T> CaseBuilder<T> cases() {
        return new CaseBuilder<>();
    }

    public static class CaseBuilder<T> {

        private Function1<? super IApplTerm, ? extends T> onAppl = null;
        private Function1<? super IListTerm, ? extends T> onList = null;
        private Function1<? super IStringTerm, ? extends T> onString = null;
        private Function1<? super IIntTerm, ? extends T> onInt = null;
        private Function1<? super IBlobTerm, ? extends T> onBlob = null;
        private Function1<? super ITermVar, ? extends T> onVar = null;

        public CaseBuilder<T> appl(Function1<? super IApplTerm, ? extends T> onAppl) {
            this.onAppl = onAppl;
            return this;
        }

        public CaseBuilder<T> list(Function1<? super IListTerm, ? extends T> onList) {
            this.onList = onList;
            return this;
        }

        public CaseBuilder<T> string(Function1<? super IStringTerm, ? extends T> onString) {
            this.onString = onString;
            return this;
        }

        public CaseBuilder<T> integer(Function1<? super IIntTerm, ? extends T> onInt) {
            this.onInt = onInt;
            return this;
        }

        public CaseBuilder<T> blob(Function1<? super IBlobTerm, ? extends T> onBlob) {
            this.onBlob = onBlob;
            return this;
        }

        public CaseBuilder<T> var(Function1<? super ITermVar, ? extends T> onVar) {
            this.onVar = onVar;
            return this;
        }

        public ITerm.Cases<T> otherwise(Function1<? super ITerm, ? extends T> otherwise) {
            return new ITerm.Cases<T>() {

                @Override public T caseAppl(IApplTerm appl) {
                    return onAppl != null ? onAppl.apply(appl) : otherwise.apply(appl);
                }

                @Override public T caseList(IListTerm list) {
                    return onList != null ? onList.apply(list) : otherwise.apply(list);
                }

                @Override public T caseString(IStringTerm string) {
                    return onString != null ? onString.apply(string) : otherwise.apply(string);
                }

                @Override public T caseInt(IIntTerm integer) {
                    return onInt != null ? onInt.apply(integer) : otherwise.apply(integer);
                }

                @Override public T caseBlob(IBlobTerm blob) {
                    return onBlob != null ? onBlob.apply(blob) : otherwise.apply(blob);
                }

                @Override public T caseVar(ITermVar var) {
                    return onVar != null ? onVar.apply(var) : otherwise.apply(var);
                }

            };
        }

    }

    public static <T> ITerm.Cases<T> casesFix(
    // @formatter:off
        Function2<ITerm.Cases<T>, ? super IApplTerm, ? extends T> onAppl,
        Function2<ITerm.Cases<T>, ? super IListTerm, ? extends T> onList,
        Function2<ITerm.Cases<T>, ? super IStringTerm, ? extends T> onString,
        Function2<ITerm.Cases<T>, ? super IIntTerm, ? extends T> onInt,
        Function2<ITerm.Cases<T>, ? super IBlobTerm, ? extends T> onBlob,
        Function2<ITerm.Cases<T>, ? super ITermVar, ? extends T> onVar
        // @formatter:on
    ) {
        return new ITerm.Cases<T>() {

            @Override public T caseAppl(IApplTerm appl) {
                return onAppl.apply(this, appl);
            }

            @Override public T caseList(IListTerm list) {
                return onList.apply(this, list);
            }

            @Override public T caseString(IStringTerm string) {
                return onString.apply(this, string);
            }

            @Override public T caseInt(IIntTerm integer) {
                return onInt.apply(this, integer);
            }

            @Override public T caseBlob(IBlobTerm blob) {
                return onBlob.apply(this, blob);
            }

            @Override public T caseVar(ITermVar var) {
                return onVar.apply(this, var);
            }

        };
    }

    // CHECKED

    public static <T, E extends Throwable> ITerm.CheckedCases<T, E> checkedCases(
    // @formatter:off
            CheckedFunction1<? super IApplTerm, T, E> onAppl, CheckedFunction1<? super IListTerm, T, E> onList,
            CheckedFunction1<? super IStringTerm, T, E> onString, CheckedFunction1<? super IIntTerm, T, E> onInt,
            CheckedFunction1<? super IBlobTerm, T, E> onBlob, CheckedFunction1<? super ITermVar, T, E> onVar
    // @formatter:on
    ) {
        return new ITerm.CheckedCases<T, E>() {

            @Override public T caseAppl(IApplTerm applTerm) throws E {
                return onAppl.apply(applTerm);
            }

            @Override public T caseList(IListTerm list) throws E {
                return onList.apply(list);
            }

            @Override public T caseString(IStringTerm string) throws E {
                return onString.apply(string);
            }

            @Override public T caseInt(IIntTerm integer) throws E {
                return onInt.apply(integer);
            }

            @Override public T caseBlob(IBlobTerm blob) throws E {
                return onBlob.apply(blob);
            }

            @Override public T caseVar(ITermVar var) throws E {
                return onVar.apply(var);
            }

        };
    }

    public static <T, E extends Throwable> CheckedCaseBuilder<T, E> checkedCases() {
        return new CheckedCaseBuilder<>();
    }

    public static class CheckedCaseBuilder<T, E extends Throwable> {

        private CheckedFunction1<? super IApplTerm, ? extends T, E> onAppl = null;
        private CheckedFunction1<? super IListTerm, ? extends T, E> onList = null;
        private CheckedFunction1<? super IStringTerm, ? extends T, E> onString = null;
        private CheckedFunction1<? super IIntTerm, ? extends T, E> onInt = null;
        private CheckedFunction1<? super IBlobTerm, ? extends T, E> onBlob = null;
        private CheckedFunction1<? super ITermVar, ? extends T, E> onVar = null;

        public CheckedCaseBuilder<T, E> appl(CheckedFunction1<? super IApplTerm, ? extends T, E> onAppl) {
            this.onAppl = onAppl;
            return this;
        }

        public CheckedCaseBuilder<T, E> list(CheckedFunction1<? super IListTerm, ? extends T, E> onList) {
            this.onList = onList;
            return this;
        }

        public CheckedCaseBuilder<T, E> string(CheckedFunction1<? super IStringTerm, ? extends T, E> onString) {
            this.onString = onString;
            return this;
        }

        public CheckedCaseBuilder<T, E> integer(CheckedFunction1<? super IIntTerm, ? extends T, E> onInt) {
            this.onInt = onInt;
            return this;
        }

        public CheckedCaseBuilder<T, E> blob(CheckedFunction1<? super IBlobTerm, ? extends T, E> onBlob) {
            this.onBlob = onBlob;
            return this;
        }

        public CheckedCaseBuilder<T, E> var(CheckedFunction1<? super ITermVar, ? extends T, E> onVar) {
            this.onVar = onVar;
            return this;
        }

        public ITerm.CheckedCases<T, E> otherwise(CheckedFunction1<? super ITerm, ? extends T, E> otherwise) {
            return new ITerm.CheckedCases<T, E>() {

                @Override public T caseAppl(IApplTerm appl) throws E {
                    return onAppl != null ? onAppl.apply(appl) : otherwise.apply(appl);
                }

                @Override public T caseList(IListTerm list) throws E {
                    return onList != null ? onList.apply(list) : otherwise.apply(list);
                }

                @Override public T caseString(IStringTerm string) throws E {
                    return onString != null ? onString.apply(string) : otherwise.apply(string);
                }

                @Override public T caseInt(IIntTerm integer) throws E {
                    return onInt != null ? onInt.apply(integer) : otherwise.apply(integer);
                }

                @Override public T caseBlob(IBlobTerm blob) throws E {
                    return onBlob != null ? onBlob.apply(blob) : otherwise.apply(blob);
                }

                @Override public T caseVar(ITermVar var) throws E {
                    return onVar != null ? onVar.apply(var) : otherwise.apply(var);
                }

            };
        }

    }


    public static String escapeString(String text) {
        final StringBuilder sb = new StringBuilder();
        final StringCharacterIterator it = new StringCharacterIterator(text);
        while(it.current() != CharacterIterator.DONE) {
            char c = it.current();
            switch(c) {
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '"':
                case '\\':
                    sb.append('\\').append(c);
                    break;
                default:
                    sb.append(c);
                    break;
            }
            it.next();
        }
        return sb.toString();
    }

    public static String unescapeString(String text) {
        final StringBuilder sb = new StringBuilder();
        final StringCharacterIterator it = new StringCharacterIterator(text);
        while(it.current() != CharacterIterator.DONE) {
            char c1 = it.current();
            if(c1 == '\\') {
                char c2 = it.next();
                if(c2 != CharacterIterator.DONE) {
                    switch(c2) {
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case '"':
                        case '\\':
                            sb.append(c2);
                            break;
                        default:
                            sb.append(c1).append(c2);
                            break;
                    }
                } else {
                    sb.append(c1);
                }
            } else {
                sb.append(c1);
            }
            it.next();
        }
        return sb.toString();
    }


    /**
     * Apply the given function to the list of terms. Returns a list of results iff any of the subterms changed
     * identity, otherwise returns null.
     */
    public static @Nullable ImList.Immutable<ITerm> applyLazy(List<ITerm> terms, Function1<ITerm, ITerm> f) {
        ImList.Mutable<ITerm> newTerms = null;
        for(int i = 0; i < terms.size(); i++) {
            ITerm term = terms.get(i);
            ITerm newTerm = f.apply(term);
            if(newTerm != term || newTerms != null) {
                if(newTerms == null) {
                    newTerms = new ImList.Mutable<>(terms.size());
                    for(int j = 0; j < i; j++) {
                        newTerms.add(terms.get(j));
                    }
                }
                newTerms.add(newTerm);
            }
        }
        return newTerms == null ? null : newTerms.freeze();
    }

}
