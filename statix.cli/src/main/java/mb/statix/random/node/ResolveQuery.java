package mb.statix.random.node;

import java.util.Optional;
import java.util.Random;

import org.metaborg.core.MetaborgException;

import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CResolveQuery;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;

public class ResolveQuery extends SearchNode<Tuple2<SearchState, CResolveQuery>, SearchState> {

    public ResolveQuery(Random rnd) {
        super(rnd);
    }

    private CResolveQuery query;

    @Override protected void doInit() {
        this.query = input._2();
    }

    @Override protected Optional<SearchState> doNext() throws MetaborgException {
        return Optional.empty();
    }

    @Override public String toString() {
        return "resolve-query";
    }

}