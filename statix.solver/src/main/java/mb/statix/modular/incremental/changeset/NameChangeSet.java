package mb.statix.modular.incremental.changeset;

import static mb.statix.modular.module.ModuleCleanliness.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import mb.statix.modular.incremental.Flag;
import mb.statix.modular.module.ModuleCleanliness;
import mb.statix.modular.solver.Context;

public class NameChangeSet extends AChangeSet {
    private static final long serialVersionUID = 1L;
    
    private static final ModuleCleanliness[] SUPPORTED = new ModuleCleanliness[] {
            CLEAN,
            DELETED,
            DIRTY,
            NEW
    };
    
    public NameChangeSet(Context oldContext,
            Collection<String> added, Collection<String> changed, Collection<String> removed) {
        super(oldContext, Arrays.asList(SUPPORTED), added, changed, removed);
        init(oldContext);
    }
    
    @Override
    protected void init(Context oldContext) {
        //1. Transitively flag removed children
        new HashSet<>(removed()).stream().flatMap(m -> m.getDescendants()).forEach(
                m -> add(Flag.DELETED, FlagCondition.OverrideFlag, m));
        
        //and dirty children
        new HashSet<>(dirty()).stream().flatMap(m -> m.getDescendants()).forEach(
                m -> add(new Flag(DIRTY, 1), FlagCondition.OverrideFlag, m));
        
        //3. Flag all the remaining modules as clean.
        add(Flag.CLEAN, FlagCondition.DontFlag, oldContext.getModules().stream().filter(m -> m.getTopCleanliness() == CLEAN));

        System.err.println("Based on the files, we identified:");
        System.err.println("  Removed:  (" + removed().size()        + ") " + removedIds());
        System.err.println("  Dirty:    (" + dirty().size()          + ") " + dirtyIds());
        System.err.println("  Clean:    (" + clean().size()          + ") " + cleanIds());
        
    }
}
