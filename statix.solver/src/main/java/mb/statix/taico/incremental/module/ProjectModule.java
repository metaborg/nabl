package mb.statix.taico.incremental.module;

import mb.nabl2.terms.ITerm;
import mb.statix.spec.Spec;
import mb.statix.taico.module.Module;
import mb.statix.taico.module.ModuleManager;

public class ProjectModule extends Module {

    public ProjectModule(ModuleManager manager, String id, Iterable<ITerm> labels, ITerm endOfPath,
            Iterable<ITerm> relations) {
        super(manager, id, labels, endOfPath, relations);
    }
    
    /**
     * Creates a new top level project module.
     * 
     * @param id
     *      the id of the module
     * @param spec
     *      the spec
     */
    public ProjectModule(ModuleManager manager, String id, Spec spec) {
        super(manager, id, spec);
    }

}
