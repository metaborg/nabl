package mb.scopegraph.regexp;

import java.util.Collection;

public interface IAlphabet<S> extends Iterable<S> {

    boolean contains(S s);

    int indexOf(S s);

    Collection<S> symbols();
    
}