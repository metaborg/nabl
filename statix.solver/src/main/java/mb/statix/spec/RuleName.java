package mb.statix.spec;

import java.io.Serializable;

public class RuleName implements Serializable {

    private static final long serialVersionUID = 42L;

    private final String name;

    private RuleName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isEmpty() {
        return name.isEmpty();
    }

    @Override public String toString() {
        return name;
    }

    @Override public int hashCode() {
        return name.hashCode();
    }

    @Override public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final RuleName other = (RuleName) obj;
        return name.equals(other.name);
    }

    public static RuleName of(String name) {
        return new RuleName(name);
    }

    public static RuleName empty() {
        return new RuleName("");
    }

}
