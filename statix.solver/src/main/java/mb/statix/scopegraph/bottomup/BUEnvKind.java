package mb.statix.scopegraph.bottomup;

import mb.nabl2.regexp.IRegExpMatcher;

public abstract class BUEnvKind<L, D> {

    public abstract boolean arePathsRelevant();
    
    public abstract IRegExpMatcher<L> wf();
    
    public abstract SpacedName index(D datum);

    public abstract BULabelOrder<L> order();

}