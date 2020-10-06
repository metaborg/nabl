package mb.nabl2.scopegraph.esop.bottomup;

import java.io.IOException;
import java.io.Serializable;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.esop.CriticalEdge;
import mb.nabl2.scopegraph.esop.IEsopNameResolution;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.path.IStep;
import mb.nabl2.scopegraph.terms.SpacedName;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.nabl2.util.collections.HashTrieRelation2;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation2;
import mb.nabl2.util.collections.IRelation3;

class BUCache<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution.IResolutionCache<S, L, O>, Serializable {
    private static final long serialVersionUID = 1L;

    private static final ILogger log = LoggerUtils.logger(BUCache.class);

    // fields cannot be final, because of readObject

    protected /* final */ Map.Immutable<Tuple2<SpacedName, L>, BUPathKey<L>> keys;
    protected /*final*/ Map.Immutable<BUEnvKey<S, L>, BUPathSet.Immutable<S, L, O, IDeclPath<S, L, O>>> envs;
    protected /*final*/ Set.Immutable<BUEnvKey<S, L>> completed;
    protected /*final*/ IRelation3.Immutable<BUEnvKey<S, L>, IStep<S, L, O>, BUEnvKey<S, L>> backedges;
    protected /*final*/ IRelation3.Immutable<BUEnvKey<S, L>, Tuple3<L, O, IRegExpMatcher<L>>, BUEnvKey<S, L>> backimports;
    protected /*final*/ IRelation2.Immutable<BUEnvKey<S, L>, CriticalEdge> openEdges;

    BUCache(Map.Immutable<Tuple2<SpacedName, L>, BUPathKey<L>> keys,
            Map.Immutable<BUEnvKey<S, L>, BUPathSet.Immutable<S, L, O, IDeclPath<S, L, O>>> envs,
            Set.Immutable<BUEnvKey<S, L>> completed,
            IRelation3.Immutable<BUEnvKey<S, L>, IStep<S, L, O>, BUEnvKey<S, L>> backedges,
            IRelation3.Immutable<BUEnvKey<S, L>, Tuple3<L, O, IRegExpMatcher<L>>, BUEnvKey<S, L>> backimports,
            IRelation2.Immutable<BUEnvKey<S, L>, CriticalEdge> openEdges) {
        this.keys = keys;
        this.envs = envs;
        this.completed = completed;
        this.backedges = backedges;
        this.backimports = backimports;
        this.openEdges = openEdges;
    }

    BUCache(Map<Tuple2<SpacedName, L>, BUPathKey<L>> keys, Map<BUEnvKey<S, L>, BUEnv<S, L, O, IDeclPath<S, L, O>>> envs,
            Set<BUEnvKey<S, L>> completed, IRelation3<BUEnvKey<S, L>, IStep<S, L, O>, BUEnvKey<S, L>> backedges,
            IRelation3<BUEnvKey<S, L>, Tuple3<L, O, IRegExpMatcher<L>>, BUEnvKey<S, L>> backimports,
            IRelation2<BUEnvKey<S, L>, CriticalEdge> openEdges) {
        final Map.Transient<BUEnvKey<S, L>, BUPathSet.Immutable<S, L, O, IDeclPath<S, L, O>>> _envs =
                Map.Transient.of();
        envs.forEach((e, ps) -> _envs.__put(e, ps.asChanges().addedPaths()));
        this.keys = CapsuleUtil.toMap(keys);
        this.envs = _envs.freeze();
        this.completed = CapsuleUtil.toSet(completed);
        this.backedges =
                HashTrieRelation3.Immutable.<BUEnvKey<S, L>, IStep<S, L, O>, BUEnvKey<S, L>>of().putAll(backedges);
        this.backimports = HashTrieRelation3.Immutable
                .<BUEnvKey<S, L>, Tuple3<L, O, IRegExpMatcher<L>>, BUEnvKey<S, L>>of().putAll(backimports);
        this.openEdges = HashTrieRelation2.Immutable.<BUEnvKey<S, L>, CriticalEdge>of().putAll(openEdges);
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        log.debug("Name resolution cache not serialized.");
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.keys = Map.Immutable.of();
        this.envs = Map.Immutable.of();
        this.completed = Set.Immutable.of();
        this.backedges = HashTrieRelation3.Immutable.of();
        this.backimports = HashTrieRelation3.Immutable.of();
        this.openEdges = HashTrieRelation2.Immutable.of();
    }

}