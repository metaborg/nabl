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
    private final Map<Class<? extends IDependencyDetail>, IDependencyDetail> details = new HashMap<>();
    
    /**
     * @param owner
     *      the id of the module that depends upon the dependant
     * @param dependant
     *      the id of the module that is depended upon
     * @param details
     *      the details
     */
    public Dependency(String owner, String dependant, IDependencyDetail... details) {
        this.owner = owner;
        this.dependant = dependant;
        for (IDependencyDetail detail : details) {
            this.details.put(detail.getClass(), detail);
        }
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
    public String getOwner() {
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
    public String getDepender() {
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
    public String getDependant() {
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
    public String getDependedUpon() {
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
