package mb.p_raffrayi.impl.envdiff;

import org.metaborg.util.collection.HashTrieRelation3;
import org.metaborg.util.collection.IRelation3;

public class DiffTreeBuilder<S, L, D> {

    private final S scope;

    private final IRelation3.Transient<L, S, IEnvDiff<S, L, D>> edges = HashTrieRelation3.Transient.of();

    public DiffTreeBuilder(S scope) {
        this.scope = scope;
    }

    public DiffTreeBuilder<S, L, D> addSubTree(L label, S scope, IEnvDiff<S, L, D> subTree) {
        edges.put(label, scope, subTree);
        return this;
    }

    public DiffTree<S, L, D> build() {
        return DiffTree.of(scope, edges.freeze());
    }

}
