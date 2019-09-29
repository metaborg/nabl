package mb.statix.random.strategy;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class Seq<I1, O, I2> extends SearchStrategy<I1, O> {
    private final SearchStrategy<I1, I2> s1;
    private final SearchStrategy<I2, O> s2;

    Seq(SearchStrategy<I1, I2> s1, SearchStrategy<I2, O> s2) {
        this.s1 = s1;
        this.s2 = s2;
    }

    @Override public SearchNodes<O> doApply(SearchContext ctx, I1 i1, SearchNode<?> parent) {
        final SearchNodes<I2> sn1 = s1.apply(ctx, i1, parent);
        if(!sn1.success()) {
            return SearchNodes.failure(sn1.parent(), sn1.error());
        }

        final AtomicBoolean collectErrors = new AtomicBoolean(true);
        final List<String> errors = Lists.newArrayList();
        final Iterator<SearchNode<O>> nodes = sn1.nodes().flatMap(n1 -> {
            final SearchNodes<O> sn2 = s2.apply(ctx, n1.output(), n1);
            if(collectErrors.get() && !sn2.success()) {
                final String desc = "( " + sn2.error() + " . " + n1.desc() + " )";
                errors.add(desc);
            }
            return sn2.success() ? sn2.nodes() : Stream.empty();
        }).iterator();
        if(!nodes.hasNext()) {
            final String error = errors.stream().collect(Collectors.joining(" & ", "( ", " )"));
            return SearchNodes.failure(sn1.parent(), error);
        }
        collectErrors.set(false);

        return SearchNodes.of(parent, Streams.stream(nodes));
    }

    @Override public String toString() {
        return "(" + s1.toString() + " . " + s2.toString() + ")";
    }

}