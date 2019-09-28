package mb.statix.random;

import java.util.Random;

import mb.statix.random.nodes.SearchElement;

public interface SearchContext {

    public Random rnd();

    public int nextNodeId();

    public void failure(SearchElement node);

}