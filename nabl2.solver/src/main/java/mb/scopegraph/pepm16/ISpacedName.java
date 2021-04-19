package mb.scopegraph.pepm16;

import mb.nabl2.terms.ITerm;

public interface ISpacedName {

    INamespace getNamespace();

    ITerm getName();

}