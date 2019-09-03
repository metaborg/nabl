package mb.statix.random.scopegraph;

import java.util.Optional;

import mb.statix.scopegraph.reference.ResolutionException;

public interface DataWF<D, X> {

    Optional<X> wf(D d) throws ResolutionException, InterruptedException;

}