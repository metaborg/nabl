package mb.scopegraph.regexp;

/**
 * Regular expression matcher.
 *
 * @param <S> the type of symbols
 */
public interface IRegExpMatcher<S> {

    IRegExp<S> regexp();

    IState<S> state();

    /**
     * Matches on the specified symbol.
     *
     * @param symbol the symbol to match on
     * @return a new matcher, which is empty if the match failed
     */
    IRegExpMatcher<S> match(S symbol);

    /**
     * Matches on the specified sequence of symbols.
     *
     * @param symbols the symbols to match on
     * @return a new matcher, which is empty if any of the matches failed
     */
    IRegExpMatcher<S> match(Iterable<S> symbols);

    /**
     * Whether this matcher is accepting.
     *
     * @return {@ocde true} when the matcher is accepting;
     * otherwise, {@code false}
     */
    boolean isAccepting();

    /**
     * Whether this matcher is final.
     *
     * @return {@code true} when the matcher is final;
     * otherwise, {@code false}
     */
    boolean isFinal();

    /**
     * Whether this matcher is empty.
     *
     * @return {@code true} when the matcher is final but not accepting;
     * otherwise, {@code false}
     */
    default boolean isEmpty() {
        return isFinal() && !isAccepting();
    }

}
