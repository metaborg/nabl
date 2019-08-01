package mb.statix.taico.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.SolverContext;

public class Modules {
    public static Stream<IModule> toModules(Stream<String> stream) {
        final SolverContext context = SolverContext.context();
        return stream.map(id -> context.getModuleUnchecked(id));
    }
    
    public static Set<IModule> toModules(Set<String> modules) {
        return toModules(modules.stream()).collect(Collectors.toSet());
    }
    
    public static List<IModule> toModules(List<String> modules) {
        return toModules(modules.stream()).collect(Collectors.toList());
    }
    
    public static IModule moduleUnchecked(String moduleId) {
        return SolverContext.context().getModuleUnchecked(moduleId);
    }
    
    public static IModule module(String requester, String moduleId) {
        return SolverContext.context().getModule(requester, moduleId);
    }
}
