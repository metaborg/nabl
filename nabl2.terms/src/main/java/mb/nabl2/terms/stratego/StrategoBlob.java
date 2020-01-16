package mb.nabl2.terms.stratego;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermPrinter;
import org.spoofax.terms.AbstractSimpleTerm;
import org.spoofax.terms.AbstractTermFactory;
import org.spoofax.terms.util.EmptyIterator;

public class StrategoBlob extends AbstractSimpleTerm implements IStrategoTerm {
    private static final long serialVersionUID = 1L;

    private final Object value;

    public StrategoBlob(Object obj) {
        this.value = obj;
    }

    public Object value() {
        return value;
    }
    
    // IStrategoTerm implementation

    @Override public boolean isList() {
        return false;
    }

    @Override public Iterator<IStrategoTerm> iterator() {
        return new EmptyIterator<>();
    }

    @Override public int getSubtermCount() {
        return 0;
    }

    @Override public IStrategoTerm getSubterm(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override public IStrategoTerm[] getAllSubterms() {
        return new IStrategoTerm[0];
    }

    @Override public int getTermType() {
        return IStrategoTerm.BLOB;
    }

    @SuppressWarnings("deprecation") @Override public IStrategoList getAnnotations() {
        return AbstractTermFactory.EMPTY_LIST;
    }

    @Override public boolean match(IStrategoTerm second) {
        return false;
    }

    @Override public void prettyPrint(ITermPrinter pp) {
        pp.print(toString());
    }

    @Override public String toString(int maxDepth) {
        return toString();
    }

    @Override public void writeAsString(Appendable output, int maxDepth) throws IOException {
        output.append(toString());
    }

    @Override public String toString() {
        return value.getClass().getSimpleName();
    }

    @SuppressWarnings("unchecked") public static <T> Optional<T> match(IStrategoTerm term, Class<T> blobClass) {
        if(term instanceof StrategoBlob) {
            StrategoBlob blob = (StrategoBlob) term;
            if(blobClass.isInstance(blob.value)) {
                return Optional.of((T) blob.value);
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }
    
}