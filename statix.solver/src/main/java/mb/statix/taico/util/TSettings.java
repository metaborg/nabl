package mb.statix.taico.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import mb.statix.taico.ndependencies.edge.RegexEdgeDependencyManager;
import mb.statix.taico.ndependencies.name.NameDependencyManager;
import mb.statix.taico.ndependencies.observer.IDependencyObserver;
import mb.statix.taico.ndependencies.query.QueryDependencyManager;

public class TSettings {
    /**
     * The observers for the dependencies.
     */
    @SuppressWarnings("unchecked")
    public static final Supplier<IDependencyObserver>[] DEPENDENCY_OBSERVERS = new Supplier[] {
            RegexEdgeDependencyManager::new,
            NameDependencyManager::new,
            QueryDependencyManager::new
    };
    
    /**
     * @return
     *      a list of newly created dependency observers for the dependency manager
     */
    public static List<IDependencyObserver> getDependencyObservers() {
        List<IDependencyObserver> list = new ArrayList<>(DEPENDENCY_OBSERVERS.length);
        for (Supplier<IDependencyObserver> supplier : DEPENDENCY_OBSERVERS) {
            list.add(supplier.get());
        }
        return list;
    }
}
