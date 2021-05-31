package mb.scopegraph.regexp;

import java.util.function.Function;

public interface IRegExp<S> {

    <T> T match(ICases<S, T> cases);

    interface ICases<S, T> extends Function<IRegExp<S>, T> {

        T emptySet();

        T emptyString();

        T symbol(S s);

        T concat(IRegExp<S> left, IRegExp<S> right);

        T closure(IRegExp<S> re);

        T or(IRegExp<S> left, IRegExp<S> right);

        T and(IRegExp<S> left, IRegExp<S> right);

        T complement(IRegExp<S> re);

        @Override default T apply(IRegExp<S> re) {
            return re.match(this);
        }

    }

}