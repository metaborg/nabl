package mb.nabl2.scopegraph;

import mb.nabl2.terms.ITerm;

public interface IOccurrence {

    INamespace getNamespace();

    ITerm getName();

    IOccurrenceIndex getIndex();

    ITerm getNameOrIndexOrigin();

    static boolean match(IOccurrence ref, IOccurrence decl) {
        return ref.getNamespace().equals(decl.getNamespace()) && ref.getName().equals(decl.getName());
    }

}