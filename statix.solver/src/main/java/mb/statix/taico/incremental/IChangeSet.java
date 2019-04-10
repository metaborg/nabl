package mb.statix.taico.incremental;

import java.util.Set;

import mb.statix.taico.module.IModule;

public interface IChangeSet {
    /**
     * @return
     *      all modules
     */
    Set<IModule> all();
    
    /**
     * @return
     *      all modules that have been removed
     */
    Set<IModule> removed();
    
    /**
     * @return
     *      all modules that have been changed <b>directly</b>
     */
    Set<IModule> changed();
    
    /**
     * @return
     *      all modules that have <b>not</b> been changed <b>directly</b> (all - changed - removed)
     */
    Set<IModule> unchanged();
    
    /**
     * @return
     *      all modules that definitely need to be reanalyzed (changed U dependsOn(removed))
     */
    Set<IModule> dirty();
    
    /**
     * @return
     *      all modules that depend on dirty modules and <b>might</b> need to be reanalyzed
     */
    Set<IModule> clirty();
    
    /**
     * @return
     *      all modules that definitely did not change (all - dirty - clirty)
     */
    Set<IModule> clean();
}
