package mb.nabl2.terms;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface IListTerm extends ITerm {

    <T> T match(Cases<T> cases);

    interface Cases<T> extends Function1<IListTerm, T> {

        T caseCons(IConsList cons);

        T caseNil(INilList nil);

        T caseVar(ITermVar var);

        @Override
        default T apply(IListTerm list) {
            return list.match(this);
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> extends CheckedFunction1<IListTerm, T, E> {

        T caseCons(IConsList cons) throws E;

        T caseNil(INilList nil) throws E;

        T caseVar(ITermVar var) throws E;

        @Override
        default T apply(IListTerm list) throws E {
            return list.matchOrThrow(this);
        }

    }

    @Override
    IListTerm withAttachments(ImmutableClassToInstanceMap<Object> value);

}