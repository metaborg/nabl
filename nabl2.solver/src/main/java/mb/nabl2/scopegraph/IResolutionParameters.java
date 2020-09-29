package mb.nabl2.scopegraph;

import mb.nabl2.regexp.IAlphabet;
import mb.nabl2.regexp.IRegExp;
import mb.nabl2.relations.IRelation;

public interface IResolutionParameters<L extends ILabel> {

    enum Strategy {
        SEARCH, ENVIRONMENTS
    }

    L getLabelD();

    L getLabelR();

    IAlphabet<L> getLabels();

    IRegExp<L> getPathWf();

    IRelation.Immutable<L> getSpecificityOrder();

    Strategy getStrategy();

    boolean getPathRelevance();

}