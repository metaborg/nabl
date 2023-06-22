package mb.scopegraph.schema;

public class Cardinality {

    private final Bound lower;

    private final Bound upper;

    public Cardinality(Bound lower, Bound upper) {
        this.lower = lower;
        this.upper = upper;
    }

    public Bound getLower() {
        return lower;
    }

    public Bound getUpper() {
        return upper;
    }

    public boolean contains(int n) {
        return lower.gte(n) && upper.lte(n);
    }

}
