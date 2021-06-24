package mb.scopegraph.oopsla20.diff;

import java.util.Map;
import java.util.Optional;

public final class BiMaps {

    private BiMaps() {}

    public static <E> Optional<BiMap.Immutable<E>> safeMerge(BiMap.Immutable<E> map1, BiMap.Immutable<E> map2) {
        final BiMap.Transient<E> result = map2.melt();
        for(Map.Entry<E, E> pair : map1.asMap().entrySet()) {
            if(!result.canPut(pair.getKey(), pair.getValue())) {
                return Optional.empty();
            }
            result.put(pair.getKey(), pair.getValue());
        }
        return Optional.of(result.freeze());
    }

}
