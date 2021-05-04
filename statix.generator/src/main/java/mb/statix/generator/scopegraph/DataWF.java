package mb.statix.generator.scopegraph;

import java.util.Optional;

import mb.scopegraph.oopsla20.reference.ResolutionException;
import mb.statix.spec.Spec;


public interface DataWF<D, X> {

    Optional<Optional<X>> wf(Spec spec, D d) throws ResolutionException, InterruptedException;

}