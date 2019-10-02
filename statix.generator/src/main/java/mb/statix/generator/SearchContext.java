package mb.statix.generator;

import java.util.Random;

import mb.statix.generator.nodes.SearchNodes;

public interface SearchContext {

    public Random rnd();

    public int nextNodeId();

    public void failure(SearchNodes<?> nodes);

}