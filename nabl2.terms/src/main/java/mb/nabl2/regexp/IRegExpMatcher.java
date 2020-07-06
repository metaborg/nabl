package mb.nabl2.regexp;

public interface IRegExpMatcher<S> {

    IRegExp<S> regexp();

    IRegExpMatcher<S> match(S symbol);

    IRegExpMatcher<S> match(Iterable<S> symbols);

    boolean isAccepting();

    boolean isFinal();

    default boolean isEmpty() {
        return isFinal() && !isAccepting();
    }

}