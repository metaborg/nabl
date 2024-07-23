package mb.scopegraph.oopsla20;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;

import org.metaborg.util.collection.Sets;
import org.metaborg.util.functions.PartialFunction1;

public final class ScopeGraphUtil {

    private ScopeGraphUtil() {
        // not instantiatable
    }

    public static <S, L, D> String toString(IScopeGraph<S, L, D> scopeGraph, Function1<D, D> instantiateData) {
        return toString(scopeGraph, instantiateData, PartialFunction1.never());
    }

    public static <S, L, D> String toString(IScopeGraph<S, L, D> scopeGraph, Function1<D, D> instantiateData, PartialFunction1<D, D> getAuxData) {
        // @formatter:off
        final Map<? extends Map.Entry<S, L>, ? extends Iterable<S>> sgEdges = scopeGraph.getEdges();
        final Map<S, java.util.Set<Map.Entry<L, Iterable<S>>>> groupedScopes = sgEdges.entrySet().stream().collect(
            Collectors.groupingBy(
                t -> t.getKey().getKey(), Collectors.mapping(
                    t -> {
                        final Map.Entry<L,Iterable<S>> result = new AbstractMap.SimpleImmutableEntry<>(t.getKey().getValue(), t.getValue());
                        return result;
                    }, Collectors.toSet()
                )
            )
        );
        // @formatter:off

        final StringBuilder sb = new StringBuilder();
        for(S source : Sets.union(groupedScopes.keySet(), scopeGraph.getData().keySet())) {
            sb.append(source);
            if(scopeGraph.getData(source).isPresent()) {
                sb.append(" : ");
                D data = scopeGraph.getData(source).get();
                final D instantatedData = instantiateData.apply(data);
                sb.append(instantatedData);
                getAuxData.apply(instantatedData).ifPresent(a -> {
                    sb.append("{");
                    sb.append(a);
                    sb.append("}");
                });
            }
            if(!groupedScopes.containsKey(source)) {
                sb.append("\n");
                continue;
            }
            sb.append(" {\n");
            for(Map.Entry<L, Iterable<S>> edges: groupedScopes.get(source)) {
                sb.append("  ");
                sb.append(edges.getKey());
                sb.append(": ");
                int indent = edges.getKey().toString().length() + 6;
                boolean first = true;
                for(S target: edges.getValue()) {
                    if(first) {
                        sb.append("[ ");
                        first = false;
                    } else {
                        sb.append(",\n");
                        sb.append(String.join("", Collections.nCopies(indent, " ")));
                    }
                    sb.append(target);
                }
                sb.append("]\n");
            }
            sb.append("}\n");
        }

        return sb.toString();
    }

}
