package mb.statix.modular.solver.concurrent;

import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;

import mb.statix.modular.solver.completeness.RedirectingIncrementalCompleteness;
import mb.statix.spec.Spec;

public class ConcurrentRedirectingIncrementalCompleteness extends RedirectingIncrementalCompleteness {

    public ConcurrentRedirectingIncrementalCompleteness(String owner, Spec spec) {
        super(owner, spec, new ConcurrentHashMap<>());
    }
    
    @Override
    protected <T> Multiset<T> createMultiset() {
        return ConcurrentHashMultiset.create();
    }
	
	//TODO IMPORTANT Also override the add method to perform atomically wrt checking if the edge is already resolved from the observer
}
