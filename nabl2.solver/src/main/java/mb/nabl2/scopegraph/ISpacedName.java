package mb.nabl2.scopegraph;

import mb.nabl2.terms.ITerm;

public interface ISpacedName {

    INamespace getNamespace();

    ITerm getName();

}