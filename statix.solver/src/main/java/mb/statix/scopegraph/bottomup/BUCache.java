package mb.statix.scopegraph.bottomup;

//class BUCache<S, L, D> implements INameResolution<S, L, D>, Serializable {
//    private static final long serialVersionUID = 1L;
//
//    private static final ILogger log = LoggerUtils.logger(BUCache.class);
//
//    // fields cannot be final, because of readObject
//
//    protected /* final */ Map.Immutable<Tuple3<BUEnvKind<L, D>, S, IRegExp<L>>, BUEnvKey<S, L, D>> envKeys;
//    protected /* final */ Map.Immutable<Tuple2<SpacedName, L>, BUPathKey<L>> pathKeys;
//    protected /*final*/ Map.Immutable<BUEnvKey<S, L, D>, BUPathSet.Immutable<S, L, D, IResolutionPath<S, L, D>>> envs;
//    protected /*final*/ Set.Immutable<BUEnvKey<S, L, D>> completed;
//    protected /*final*/ IRelation3.Immutable<BUEnvKey<S, L, D>, IStep<S, L>, BUEnvKey<S, L, D>> backedges;
//    protected /*final*/ IRelation3.Immutable<BUEnvKey<S, L, D>, Tuple3<L, D, IRegExpMatcher<L>>, BUEnvKey<S, L, D>> backimports;
//    protected /*final*/ IRelation2.Immutable<BUEnvKey<S, L, D>, CriticalEdge> openEdges;
//
//    BUCache(Map.Immutable<Tuple3<BUEnvKind<L, D>, S, IRegExp<L>>, BUEnvKey<S, L, D>> envKeys,
//            Map.Immutable<Tuple2<SpacedName, L>, BUPathKey<L>> pathKeys,
//            Map.Immutable<BUEnvKey<S, L, D>, BUPathSet.Immutable<S, L, D, IResolutionPath<S, L, D>>> envs,
//            Set.Immutable<BUEnvKey<S, L, D>> completed,
//            IRelation3.Immutable<BUEnvKey<S, L, D>, IStep<S, L>, BUEnvKey<S, L, D>> backedges,
//            IRelation3.Immutable<BUEnvKey<S, L, D>, Tuple3<L, O, IRegExpMatcher<L>>, BUEnvKey<S, L, D>> backimports,
//            IRelation2.Immutable<BUEnvKey<S, L, D>, CriticalEdge> openEdges) {
//        this.envKeys = envKeys;
//        this.pathKeys = pathKeys;
//        this.envs = envs;
//        this.completed = completed;
//        this.backedges = backedges;
//        this.backimports = backimports;
//        this.openEdges = openEdges;
//    }
//
//    BUCache(Map<Tuple3<BUEnvKind<L, D>, S, IRegExp<L>>, BUEnvKey<S, L, D>> envKeys,
//            Map<Tuple2<SpacedName, L>, BUPathKey<L>> pathKeys,
//            Map<BUEnvKey<S, L, D>, BUEnv<S, L, D, IResolutionPath<S, L, D>>> envs, Set<BUEnvKey<S, L, D>> completed,
//            IRelation3<BUEnvKey<S, L, D>, IStep<S, L>, BUEnvKey<S, L, D>> backedges,
//            IRelation3<BUEnvKey<S, L, D>, Tuple3<L, O, IRegExpMatcher<L>>, BUEnvKey<S, L, D>> backimports,
//            IRelation2<BUEnvKey<S, L, D>, CriticalEdge> openEdges) {
//        final Map.Transient<BUEnvKey<S, L, D>, BUPathSet.Immutable<S, L, D, IResolutionPath<S, L, D>>> _envs =
//                Map.Transient.of();
//        envs.forEach((e, ps) -> _envs.__put(e, ps.pathSet()));
//        this.envKeys = CapsuleUtil.toMap(envKeys);
//        this.pathKeys = CapsuleUtil.toMap(pathKeys);
//        this.envs = _envs.freeze();
//        this.completed = CapsuleUtil.toSet(completed);
//        this.backedges =
//                HashTrieRelation3.Immutable.<BUEnvKey<S, L, D>, IStep<S, L>, BUEnvKey<S, L, D>>of().putAll(backedges);
//        this.backimports = HashTrieRelation3.Immutable
//                .<BUEnvKey<S, L, D>, Tuple3<L, O, IRegExpMatcher<L>>, BUEnvKey<S, L, D>>of().putAll(backimports);
//        this.openEdges = HashTrieRelation2.Immutable.<BUEnvKey<S, L, D>, CriticalEdge>of().putAll(openEdges);
//    }
//
//    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//        log.debug("Name resolution cache not serialized.");
//    }
//
//    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//        this.envKeys = Map.Immutable.of();
//        this.pathKeys = Map.Immutable.of();
//        this.envs = Map.Immutable.of();
//        this.completed = Set.Immutable.of();
//        this.backedges = HashTrieRelation3.Immutable.of();
//        this.backimports = HashTrieRelation3.Immutable.of();
//        this.openEdges = HashTrieRelation2.Immutable.of();
//    }
//
//}