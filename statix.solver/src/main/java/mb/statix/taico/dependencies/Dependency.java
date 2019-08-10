package mb.statix.taico.dependencies;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import mb.statix.taico.dependencies.details.IDependencyDetail;
import mb.statix.taico.util.TPrettyPrinter;

/**
 * Class to model a dependency {@code A -> B} with details.
 * <p>
 * This class is immutable.
 */
public class Dependency implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String owner;
    private final String dependant;
    private final Map<Class<? extends IDependencyDetail>, IDependencyDetail> details;
    
    /**
     * @param owner
     *      the owner
     * @param dependant
     *      the dependant
     * @param details
     *      the dependency details
     */
    public Dependency(String owner, String dependant, Map<Class<? extends IDependencyDetail>, IDependencyDetail> details) {
        this.owner = owner;
        this.dependant = dependant;
        this.details = details;
    }
    
    /**
     * @param owner
     *      the owner
     * @param dependant
     *      the dependant
     * @param details
     *      the dependency details
     */
    public Dependency(String owner, String dependant, IDependencyDetail... details) {
        this(owner, dependant, new HashMap<>());
        for (IDependencyDetail detail : details) {
            this.details.put(detail.getClass(), detail);
        }
    }
    
    /**
     * Protected constructor for {@link FakeDependency}.
     * 
     * @param owner
     *      the owner
     */
    protected Dependency(String owner) {
        this.owner = owner;
        this.dependant = null;
        this.details = null;
    }

    /**
     * For a dependency relation A -> B, this method returns A.
     * 
     * @return
     *      the id of the module that depends upon another module
     * 
     * @see #getDepender()
     *      This method is the same as getDepender()
     */
    public final String getOwner() {
        return owner;
    }
    
    /**
     * For a dependency relation A -> B, this method returns A.
     * 
     * @return
     *      the id of the module that depends upon another module
     * 
     * @see #getOwner()
     *      This method is the same as getOwner()
     */
    public final String getDepender() {
        return owner;
    }
    
    /**
     * For a dependency relation A -> B, this method returns B.
     * 
     * @return
     *      the id of the module that is depended upon
     * 
     * @see #getDependedUpon()
     *      This method is the same as getDependedUpon()
     */
    public final String getDependant() {
        return dependant;
    }
    
    /**
     * For a dependency relation A -> B, this method returns B.
     * 
     * @return
     *      the id of the module that is depended upon
     * 
     * @see #getDependant()
     *      This method is the same as getDependant()
     */
    public final String getDependedUpon() {
        return dependant;
    }
    
    /**
     * @param clazz
     *      the type of details
     * 
     * @return
     *      the details of the given type, or null if not available
     */
    @SuppressWarnings("unchecked")
    public <T extends IDependencyDetail> T getDetails(Class<T> clazz) {
        return (T) details.get(clazz);
    }
    
    /**
     * @param clazz
     *      the type of details
     * 
     * @return
     *      true if the given type of details are available, false otherwise
     */
    public <T extends IDependencyDetail> boolean hasDetails(Class<T> clazz) {
        return details.containsKey(clazz);
    }
    
    /**
     * @return
     *      the details of this dependency
     */
    public Collection<IDependencyDetail> getDetails() {
        return details.values();
    }
    
    // --------------------------------------------------------------------------------------------
    // Debugging methods
    // --------------------------------------------------------------------------------------------
    
    public String print(boolean pretty) {
        StringBuilder sb = new StringBuilder();
        sb.append(pretty ? TPrettyPrinter.printModule(owner) : owner);
        sb.append(" -> ");
        sb.append(pretty ? TPrettyPrinter.printModule(dependant) : dependant);
        if (!details.isEmpty()) {
            sb.append(" | <");
            String detailString = details.values().stream()
                    .map(IDependencyDetail::toString)
                    .collect(Collectors.joining(", "));
            sb.append(detailString);
            sb.append(">");
        }
        return sb.toString();
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public String toString() {
        if (details.isEmpty()) {
            return owner + " -> " + dependant;
        }
        
        String detailString = details.values().stream()
                .map(IDependencyDetail::toString)
                .collect(Collectors.joining(", "));
        
        return owner + " -> " + dependant + " | <" + detailString + ">";
    }
    
    // --------------------------------------------------------------------------------------------
    // Other
    // --------------------------------------------------------------------------------------------
    
    /**
     * Checks if this dependency is equal to the given dependency, excluding the dependant.
     * 
     * @param other
     *      the other dependency
     * 
     * @return
     *      true if equal excluding the dependant, false otherwise
     */
    public boolean equalsExcludingDependant(Dependency other) {
        if (other == this) return true;
        if (!this.owner.equals(other.owner)) return false;
        if (!this.details.equals(other.details)) return false;
        
        return true;
    }
}
