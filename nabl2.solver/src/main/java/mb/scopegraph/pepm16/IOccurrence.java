package mb.scopegraph.pepm16;

import mb.nabl2.terms.ITerm;
import mb.scopegraph.pepm16.terms.SpacedName;

public interface IOccurrence {

    INamespace getNamespace();

    ITerm getName();

    IOccurrenceIndex getIndex();

    ITerm getNameOrIndexOrigin();

    SpacedName getSpacedName();

    static boolean match(IOccurrence ref, IOccurrence decl) {
        return ref.getNamespace().equals(decl.getNamespace()) && ref.getName().equals(decl.getName());
    }

}