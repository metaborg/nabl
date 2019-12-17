package mb.nabl2.scopegraph;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import mb.nabl2.scopegraph.path.IResolutionPath;


public interface INameResolution<S extends IScope, L extends ILabel, O extends IOccurrence> {

    java.util.Set<O> getResolvedRefs();

    Optional<Set<IResolutionPath<S, L, O>>> resolve(O ref);

    Optional<Set<O>> decls(S scope);

    Optional<Set<O>> refs(S scope);

    Optional<Set<O>> visible(S scope);

    Optional<Set<O>> reachable(S scope);

    Set<Map.Entry<O, Set<IResolutionPath<S, L, O>>>> resolutionEntries();

}