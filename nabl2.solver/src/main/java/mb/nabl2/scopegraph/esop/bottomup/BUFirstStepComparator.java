package mb.nabl2.scopegraph.esop.bottomup;

import mb.nabl2.relations.IRelation;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.path.IScopePath;

public class BUFirstStepComparator<S extends IScope, L extends ILabel, O extends IOccurrence> {

    private final L labelD;
    private final IRelation<L> order;

    public BUFirstStepComparator(L labelD, IRelation<L> order) {
        this.labelD = labelD;
        this.order = order;
    }

    public Integer compare(IResolutionPath<S, L, O> path1, IResolutionPath<S, L, O> path2) {
        if(order.isEmpty()) {
            return path1.equals(path2) ? 0 : null;
        }
        return compare(path1, path2, order);
    }

    private Integer compare(IResolutionPath<S, L, O> path1, IResolutionPath<S, L, O> path2, IRelation<L> order) {
        if(!path1.getReference().equals(path2.getReference())) {
            return null;
        }
        return compare(path1.getPath(), path2.getPath(), order);
    }

    public Integer compare(IDeclPath<S, L, O> path1, IDeclPath<S, L, O> path2) {
        if(order.isEmpty()) {
            return path1.equals(path2) ? 0 : null;
        }
        return compare(path1, path2, order);
    }

    private Integer compare(IDeclPath<S, L, O> path1, IDeclPath<S, L, O> path2, IRelation<L> order) {
        if(!path1.getDeclaration().getSpacedName().equals(path2.getDeclaration().getSpacedName())) {
            return null;
        }
        return compare(path1.getPath(), path2.getPath(), order);
    }

    private Integer compare(IScopePath<S, L, O> path1, IScopePath<S, L, O> path2, IRelation<L> order) {
        if(!path1.getSource().equals(path2.getSource())) {
            // paths with different sources are unordered
            return null;
        }
        final L l1 = path1.getFirstLabel(labelD);
        final L l2 = path2.getFirstLabel(labelD);
        return compare(l1, l2);
    }

    private Integer compare(L l1, L l2) {
        if(l1.equals(l2)) {
            return 0;
        } else if(order.contains(l1, l2)) {
            return -1;
        } else if(order.contains(l2, l1)) {
            return 1;
        } else {
            return null;
        }
    }

}