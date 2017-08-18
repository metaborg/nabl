package meta.flowspec.java.pcollections;

import java.util.Map;
import java.util.Set;

import org.metaborg.meta.nabl2.util.collections.IRelation2;
import org.metaborg.meta.nabl2.util.collections.ISet;
import org.metaborg.meta.nabl2.util.collections.WrappedSet;
import org.pcollections.PSet;

public interface PRelation<L, R> extends IRelation2<L, R> {
    PRelation<L,R> plus(L lhs, R rhs);
    PRelation<L,R> plusAll(PRelation<? extends L, ? extends R> map);
    PRelation<L,R> plusAll(Map<? extends L, ? extends R> map);
    PRelation<L,R> minus(L lhs, R rhs);
    PRelation<L,R> minusAll(PRelation<? extends L, ? extends R> map);
    PRelation<L,R> minusAll(Map<? extends L, ? extends R> map);

    int size();
    boolean isEmpty();
    boolean contains(L lhs, R rhs);
    boolean containsLhs(L lhs);
    boolean containsRhs(R rhs);
    PSet<R> getRhsSet(L lhs);
    Set<L> lhsSet();
    PSet<R> rhsSet();
    Set<Map.Entry<L, R>> entrySet();

    PRelation<R,L> reverse();

    @Override
    public default IRelation2<R, L> inverse() {
        return this.reverse();
    }

    @Override
    public default boolean containsKey(L key) {
        return this.containsLhs(key);
    }

    @Override
    public default boolean containsEntry(L key, R value) {
        return this.contains(key, value);
    }

    @Override
    public default boolean containsValue(R value) {
        return this.containsRhs(value);
    }

    @Override
    public default ISet<L> keySet() {
        return WrappedSet.of(this.lhsSet());
    }

    @Override
    public default ISet<R> valueSet() {
        return WrappedSet.of(this.rhsSet());
    }

    @Override
    public default ISet<R> get(L key) {
        return WrappedSet.of(this.getRhsSet(key));
    }
}
