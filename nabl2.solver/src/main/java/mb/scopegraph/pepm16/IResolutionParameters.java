package mb.scopegraph.pepm16;

import mb.scopegraph.regexp.IAlphabet;
import mb.scopegraph.regexp.IRegExp;
import mb.scopegraph.relations.IRelation;

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