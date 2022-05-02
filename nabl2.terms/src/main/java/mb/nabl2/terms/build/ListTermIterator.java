package mb.nabl2.terms.build;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.IConsTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.INilTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

public class ListTermIterator implements Iterator<ITerm> {

    private IListTerm current;

    public ListTermIterator(IListTerm list) {
        this.current = list;
    }

    @Override public boolean hasNext() {
        IListTerm subj = current;
        switch(subj.listTermTag()) {
            case IConsTerm: {
                return true;
            }

            case INilTerm: {
                return false;
            }

            case ITermVar: {
                throw new IllegalStateException("Cannot iterate over a non-ground list.");
            }
        }
        // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
        throw new RuntimeException("Missing case for IListTerm subclass/tag");
    }

    @Override public ITerm next() {
        IListTerm subj = current;
        switch(subj.listTermTag()) {
            case IConsTerm: { IConsTerm cons = (IConsTerm) subj;
                current = cons.getTail();
                return cons.getHead();
            }

            case INilTerm: {
                throw new NoSuchElementException();
            }

            case ITermVar: {
                throw new IllegalStateException("Cannot iterate over a non-ground list.");
            }
        }
        // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
        throw new RuntimeException("Missing case for IListTerm subclass/tag");
    }

}