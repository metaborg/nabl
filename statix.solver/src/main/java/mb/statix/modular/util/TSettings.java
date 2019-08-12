package mb.statix.modular.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import mb.statix.modular.ndependencies.data.RegexDataDependencyManager;
import mb.statix.modular.ndependencies.edge.RegexEdgeDependencyManager;
import mb.statix.modular.ndependencies.name.NameDependencyManager;
import mb.statix.modular.ndependencies.observer.IDependencyObserver;
import mb.statix.modular.ndependencies.query.QueryDependencyManager;

public class TSettings {
    /**
     * The observers for the dependencies.
     */
    @SuppressWarnings("unchecked")
    public static final Supplier<IDependencyObserver>[] DEPENDENCY_OBSERVERS = new Supplier[] {
            RegexEdgeDependencyManager::new,
            NameDependencyManager::new,
            QueryDependencyManager::new,
            () -> new RegexDataDependencyManager(true)
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
