package mb.nabl2.terms;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

public interface IListTerm extends ITerm {

    /** Gets the minimum size of the list. This is the size of the list if the list does not contain a variable term as a tail. */
    int getMinSize();

    <T> T match(Cases<T> cases);

    interface Cases<T> extends Function1<IListTerm, T> {

        T caseCons(IConsTerm cons);

        T caseNil(INilTerm nil);

        T caseVar(ITermVar var);

        @Override
        default T apply(IListTerm list) {
            return list.match(this);
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> extends CheckedFunction1<IListTerm, T, E> {

        T caseCons(IConsTerm cons) throws E;

        T caseNil(INilTerm nil) throws E;

        T caseVar(ITermVar var) throws E;

        @Override
        default T apply(IListTerm list) throws E {
            return list.matchOrThrow(this);
        }

    }

    @Override
    IListTerm withAttachments(IAttachments value);

    Tag listTermTag();

    enum Tag {
        IConsTerm,
        INilTerm,
        ITermVar
    }

}
