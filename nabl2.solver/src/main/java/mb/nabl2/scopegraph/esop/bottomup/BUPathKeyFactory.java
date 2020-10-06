package mb.nabl2.scopegraph.esop.bottomup;

import io.usethesource.capsule.Map;
import mb.nabl2.scopegraph.terms.SpacedName;
import mb.nabl2.util.Tuple2;

public class BUPathKeyFactory<L> {

    private Map.Transient<Tuple2<SpacedName, L>, Tuple2<SpacedName, L>> keys = Map.Transient.of();

    public Tuple2<SpacedName, L> build(SpacedName name, L label) {
        Tuple2<SpacedName, L> key = Tuple2.of(name, label);
        Tuple2<SpacedName, L> result;
        if((result = keys.get(key)) == null) {
            keys.put(key, result = key);
        }
        return result;
    }

}
