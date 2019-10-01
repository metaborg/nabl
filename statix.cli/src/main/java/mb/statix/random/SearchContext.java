package mb.statix.random;

import java.util.Random;

import mb.statix.random.nodes.SearchNodes;

public interface SearchContext {

    public Random rnd();

    public int nextNodeId();

    public void failure(SearchNodes<?> nodes);

}