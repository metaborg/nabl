package mb.statix.generator;

import java.util.Random;

import mb.statix.generator.nodes.SearchNodes;
import mb.statix.spec.Spec;


public interface SearchContext {

    Spec spec();

    Random rnd();

    int nextNodeId();

    void failure(SearchNodes<?> nodes);

}