package meta.flowspec.java.interpreter.values;

public class Tuple {
    private final Object[] children;

    public Tuple(Object[] children) {
        this.children = children;
    }

    public Object[] getChildren() {
        return children;
    }
}
