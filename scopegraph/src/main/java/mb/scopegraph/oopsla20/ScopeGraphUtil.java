package mb.scopegraph.oopsla20;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public final class ScopeGraphUtil {

    private ScopeGraphUtil() {
        // not instantiatable
    }

    public static <S, L, D> String toString(IScopeGraph<S, L, D> scopeGraph, Function1<D, D> instantiateData) {
     // @formatter:off
        final Map<S, java.util.Set<Map.Entry<L, Iterable<S>>>> groupedScopes = scopeGraph.getEdges().entrySet().stream()
                .collect(Collectors.groupingBy(t -> t.getKey().getKey(), Collectors.mapping(
                        t -> (Map.Entry<L, Iterable<S>>)new AbstractMap.SimpleImmutableEntry<L, Iterable<S>>(t.getKey().getValue(), t.getValue()), Collectors.toSet())));
        // @formatter:off

        final SetView<S> scopes = Sets.union(groupedScopes.keySet(), scopeGraph.getData().keySet());

        final StringBuilder sb = new StringBuilder();
        for(S source : scopes) {
            sb.append(source);
            if(scopeGraph.getData(source).isPresent()) {
                sb.append(" : ");
                sb.append(instantiateData.apply(scopeGraph.getData(source).get()));
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
