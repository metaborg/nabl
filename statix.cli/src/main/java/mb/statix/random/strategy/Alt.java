package mb.statix.random.strategy;

import java.util.Optional;

import org.metaborg.core.MetaborgException;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchNodes;
import mb.statix.random.SearchStrategy;

final class Alt<I, O1, O2> extends SearchStrategy<I, Either2<O1, O2>> {
    private final SearchStrategy<I, O2> s2;
    private final SearchStrategy<I, O1> s1;

    Alt(SearchStrategy<I, O1> s1, SearchStrategy<I, O2> s2) {
        this.s2 = s2;
        this.s1 = s1;
    }

    @Override protected SearchNodes<Either2<O1, O2>> doApply(SearchContext ctx, I input, SearchNode<?> parent) {
        final SearchNodes<O1> ns1 = s1.apply(ctx, input, parent);
        final SearchNodes<O2> ns2 = s2.apply(ctx, input, parent);
        return new SearchNodes<Either2<O1, O2>>() {

            @Override public Optional<SearchNode<Either2<O1, O2>>> next()
                    throws MetaborgException, InterruptedException {
                final boolean left = ctx.rnd().nextBoolean();
                final SearchNode<O1> n1;
                final SearchNode<O2> n2;
                final SearchNode<Either2<O1, O2>> next;
                if(left) {
                    if((n2 = ns2.next().orElse(null)) != null) {
                        final Either2<O1, O2> output = Either2.ofRight(n2.output());
                        next = new SearchNode<>(ctx.nextNodeId(), output, n2.parent(), n2.desc());
                    } else if((n1 = ns1.next().orElse(null)) != null) {
                        final Either2<O1, O2> output = Either2.ofLeft(n1.output());
                        next = new SearchNode<>(ctx.nextNodeId(), output, n1.parent(), n1.desc());
                    } else {
                        return Optional.empty();
                    }
                } else {
                    if((n1 = ns1.next().orElse(null)) != null) {
                        final Either2<O1, O2> output = Either2.ofLeft(n1.output());
                        next = new SearchNode<>(ctx.nextNodeId(), output, n1.parent(), n1.desc());
                    } else if((n2 = ns2.next().orElse(null)) != null) {
                        final Either2<O1, O2> output = Either2.ofRight(n2.output());
                        next = new SearchNode<>(ctx.nextNodeId(), output, n2.parent(), n2.desc());
                    } else {
                        return Optional.empty();
                    }
                }
                return Optional.of(next);
            }

        };

    }

    @Override public String toString() {
        return "(" + s1 + " | " + s2 + ")<";
    }

}