package mb.statix.taico.incremental;

import java.io.Serializable;

import org.checkerframework.checker.nullness.qual.Nullable;

import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.solver.Context;

public class Flag implements Serializable, Comparable<Flag> {
    private static final long serialVersionUID = 1L;
    
    public static final Flag CLEAN = new Flag(ModuleCleanliness.CLEAN, 1);
    public static final Flag NEW = new Flag(ModuleCleanliness.NEW, 1);
    public static final Flag DELETED = new Flag(ModuleCleanliness.DELETED, 1);
    public static final Flag NEWCHILD = new Flag(ModuleCleanliness.NEWCHILD, 1);
    
    private final ModuleCleanliness cleanliness;
    private final int level;
    private final String cause;
    
    public Flag(ModuleCleanliness cleanliness, int level) {
        this(cleanliness, level, null);
    }
    
    public Flag(ModuleCleanliness cleanliness, int level, @Nullable String cause) {
        this.cleanliness = cleanliness;
        this.level = level;
        this.cause = cause;
    }

    public ModuleCleanliness getCleanliness() {
        return cleanliness;
    }
    
    public int getLevel() {
        return level;
    }
    
    @Nullable
    public String getCause() {
        return cause;
    }
    
    public IModule getModuleCause() {
        return cause == null ? null : Context.context().getModuleUnchecked(cause);
    }

    @Override
    public int compareTo(Flag o) {
        int c = cleanliness.compareTo(o.cleanliness);
        if (c != 0) return c;
        
        return Integer.compare(level, o.level);
    }
    
    @Override
    public String toString() {
        return "Flag<" + cleanliness + ", " + level + ", " + cause + ">";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + cleanliness.hashCode();
        result = prime * result + level;
        result = prime * result + ((cause == null) ? 0 : cause.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Flag)) return false;
        
        Flag other = (Flag) obj;
        if (cleanliness != other.cleanliness) return false;
        if (level != other.level) return false;
        if (cause == null) {
            if (other.cause != null) {
                return false;
            }
        } else if (!cause.equals(other.cause)) {
            return false;
        }
        return true;
    }
}
