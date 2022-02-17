package mb.scopegraph.resolution;

import java.io.Serializable;

public class RVar implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;

    public RVar(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override public String toString() {
        return name;
    }

    @Override public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        return name.equals(((RVar) obj).name);
    }

    @Override public int hashCode() {
        return name.hashCode();
    }

}
