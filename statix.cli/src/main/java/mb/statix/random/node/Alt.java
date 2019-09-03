package mb.statix.random.node;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.metaborg.core.MetaborgException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mb.statix.random.SearchNode;

public class Alt<I, O> extends SearchNode<I, O> {

    private final List<SearchNode<I, O>> ns;

    @SafeVarargs public Alt(Random rnd, SearchNode<I, O>... ns) {
        super(rnd);
        this.ns = ImmutableList.copyOf(ns);
    }

    private List<SearchNode<I, O>> _ns;

    @Override protected void doInit() {
        _ns = Lists.newLinkedList(ns);
        _ns.forEach(n -> n.init(input));
    }

    @Override protected Optional<O> doNext() throws MetaborgException, InterruptedException {
        if(_ns.isEmpty()) {
            return Optional.empty();
        }
        final int idx = rnd.nextInt(_ns.size());
        final Optional<O> o = _ns.get(idx).next();
        if(!o.isPresent()) {
            _ns.remove(idx);
            return next();
        }
        return o;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("alt(");
        ns.forEach(n -> {
            sb.append(ns);
            sb.append(",");
        });
        sb.append(")");
        return sb.toString();
    }

}