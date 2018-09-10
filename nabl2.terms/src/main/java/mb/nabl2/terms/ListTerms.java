package mb.nabl2.terms;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.unit.Unit;

import mb.nabl2.terms.build.ListTermIterator;

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


    public static <T> CaseBuilder<T> cases() {
        return new CaseBuilder<>();
    }

    public static class CaseBuilder<T> {

        private Function1<? super IConsTerm, T> onCons = null;
        private Function1<? super INilTerm, T> onNil = null;
        private Function1<? super ITermVar, T> onVar = null;

        public CaseBuilder<T> cons(Function1<? super IConsTerm, T> onCons) {
            this.onCons = onCons;
            return this;
        }

        public CaseBuilder<T> nil(Function1<? super INilTerm, T> onNil) {
            this.onNil = onNil;
            return this;
        }

        public CaseBuilder<T> var(Function1<? super ITermVar, T> onVar) {
            this.onVar = onVar;
            return this;
        }

        public IListTerm.Cases<T> otherwise(final Function1<? super IListTerm, T> otherwise) {
            return new IListTerm.Cases<T>() {

                @Override public T caseCons(IConsTerm cons) {
                    return onCons != null ? onCons.apply(cons) : otherwise.apply(cons);
                }

                @Override public T caseNil(INilTerm nil) {
                    return onNil != null ? onNil.apply(nil) : otherwise.apply(nil);
                }

                @Override public T caseVar(ITermVar var) {
                    return onVar != null ? onVar.apply(var) : otherwise.apply(var);
                }

            };

        }

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

    public static <T, E extends Throwable> IListTerm.CheckedCases<T, E> checkedCases(
    // @formatter:off
        CheckedFunction1<? super IConsTerm,T,E> onCons,
        CheckedFunction1<? super INilTerm,T,E> onNil,
        CheckedFunction1<? super ITermVar,T,E> onVar
        // @formatter:on
    ) {
        return new IListTerm.CheckedCases<T, E>() {

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

    public static String toString(IListTerm list) {
        return list
                .match(cases(cons -> toStringTail(cons.getHead(), cons.getTail()), nil -> "[]", var -> var.toString()));
    }

    private static String toStringTail(ITerm head, IListTerm tail) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(head);
        tail.match(casesFix(
        // @formatter:off
            (f,cons) -> {
                sb.append(",");
                sb.append(cons.getHead());
                return cons.getTail().match(f);
            },
            (f,nil) -> Unit.unit,
            (f,var) -> {
                sb.append("|");
                sb.append(var);
                return Unit.unit;
            }
            // @formatter:on
        ));
        sb.append("]");
        return sb.toString();
    }

    public static Iterable<ITerm> iterable(IListTerm list) {
        return () -> new ListTermIterator(list);
    }

}