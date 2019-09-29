package mb.statix.random.strategy;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
        final Iterator<SearchNode<I2>> ns1 = sn1.nodes().iterator();
        if(!ns1.hasNext()) {
            return SearchNodes.empty(sn1.parent(), sn1.desc());
        }

        final AtomicBoolean collectDesc = new AtomicBoolean(true);
        final List<String> descs = Lists.newArrayList();
        final Iterator<SearchNode<O>> nodes = Streams.stream(ns1).flatMap(n1 -> {
            final SearchNodes<O> sn2 = s2.apply(ctx, n1.output(), n1);
            final Iterator<SearchNode<O>> ns2 = sn2.nodes().iterator();
            if(collectDesc.get() && !ns2.hasNext()) {
                final String desc = n1.desc() + " . " + sn2.desc();
                descs.add(desc);
            }
            return Streams.stream(ns2);
        }).iterator();
        if(!nodes.hasNext()) {
            final String desc = descs.stream().collect(Collectors.joining(" & ", "(", ")"));
            return SearchNodes.empty(sn1.parent(), desc);
        }
        collectDesc.set(false);

        return SearchNodes.of(parent, Streams.stream(nodes));
    }

    @Override public String toString() {
        return "(" + s1.toString() + " . " + s2.toString() + ")";
    }

}