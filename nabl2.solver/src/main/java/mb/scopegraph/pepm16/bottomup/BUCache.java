package mb.scopegraph.pepm16.bottomup;

import java.io.IOException;
import java.io.Serializable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.HashTrieRelation2;
import org.metaborg.util.collection.HashTrieRelation3;
import org.metaborg.util.collection.IRelation2;
import org.metaborg.util.collection.IRelation3;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.tuple.Tuple3;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.esop15.CriticalEdge;
import mb.scopegraph.pepm16.esop15.IEsopNameResolution;
import mb.scopegraph.pepm16.path.IDeclPath;
import mb.scopegraph.pepm16.path.IStep;
import mb.scopegraph.pepm16.terms.SpacedName;
import mb.scopegraph.regexp.IRegExp;
import mb.scopegraph.regexp.IRegExpMatcher;

class BUCache<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution.IResolutionCache<S, L, O>, Serializable {
    private static final long serialVersionUID = 1L;

    private static final ILogger log = LoggerUtils.logger(BUCache.class);

    // fields cannot be final, because of readObject

    protected /* final */ Map.Immutable<Tuple3<BUEnvKind, S, IRegExp<L>>, BUEnvKey<S, L>> envKeys;
    protected /* final */ Map.Immutable<Tuple2<SpacedName, L>, BUPathKey<L>> pathKeys;
    protected /*final*/ Map.Immutable<BUEnvKey<S, L>, BUPathSet.Immutable<S, L, O, IDeclPath<S, L, O>>> envs;
    protected /*final*/ Set.Immutable<BUEnvKey<S, L>> completed;
    protected /*final*/ IRelation3.Immutable<BUEnvKey<S, L>, IStep<S, L, O>, BUEnvKey<S, L>> backedges;
    protected /*final*/ IRelation3.Immutable<BUEnvKey<S, L>, Tuple3<L, O, IRegExpMatcher<L>>, BUEnvKey<S, L>> backimports;
    protected /*final*/ IRelation2.Immutable<BUEnvKey<S, L>, CriticalEdge> openEdges;

    BUCache(Map.Immutable<Tuple3<BUEnvKind, S, IRegExp<L>>, BUEnvKey<S, L>> envKeys,
            Map.Immutable<Tuple2<SpacedName, L>, BUPathKey<L>> pathKeys,
            Map.Immutable<BUEnvKey<S, L>, BUPathSet.Immutable<S, L, O, IDeclPath<S, L, O>>> envs,
            Set.Immutable<BUEnvKey<S, L>> completed,
            IRelation3.Immutable<BUEnvKey<S, L>, IStep<S, L, O>, BUEnvKey<S, L>> backedges,
            IRelation3.Immutable<BUEnvKey<S, L>, Tuple3<L, O, IRegExpMatcher<L>>, BUEnvKey<S, L>> backimports,
            IRelation2.Immutable<BUEnvKey<S, L>, CriticalEdge> openEdges) {
        this.envKeys = envKeys;
        this.pathKeys = pathKeys;
        this.envs = envs;
        this.completed = completed;
        this.backedges = backedges;
        this.backimports = backimports;
        this.openEdges = openEdges;
    }

    BUCache(Map<Tuple3<BUEnvKind, S, IRegExp<L>>, BUEnvKey<S, L>> envKeys,
            Map<Tuple2<SpacedName, L>, BUPathKey<L>> pathKeys,
            Map<BUEnvKey<S, L>, BUEnv<S, L, O, IDeclPath<S, L, O>>> envs, Set<BUEnvKey<S, L>> completed,
            IRelation3<BUEnvKey<S, L>, IStep<S, L, O>, BUEnvKey<S, L>> backedges,
            IRelation3<BUEnvKey<S, L>, Tuple3<L, O, IRegExpMatcher<L>>, BUEnvKey<S, L>> backimports,
            IRelation2<BUEnvKey<S, L>, CriticalEdge> openEdges) {
        final Map.Transient<BUEnvKey<S, L>, BUPathSet.Immutable<S, L, O, IDeclPath<S, L, O>>> _envs =
                CapsuleUtil.transientMap();
        envs.forEach((e, ps) -> _envs.__put(e, ps.pathSet()));
        this.envKeys = CapsuleUtil.toMap(envKeys);
        this.pathKeys = CapsuleUtil.toMap(pathKeys);
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
        this.envKeys = CapsuleUtil.immutableMap();
        this.pathKeys = CapsuleUtil.immutableMap();
        this.envs = CapsuleUtil.immutableMap();
        this.completed = CapsuleUtil.immutableSet();
        this.backedges = HashTrieRelation3.Immutable.of();
        this.backimports = HashTrieRelation3.Immutable.of();
        this.openEdges = HashTrieRelation2.Immutable.of();
    }

}