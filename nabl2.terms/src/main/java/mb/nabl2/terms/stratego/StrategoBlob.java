package mb.nabl2.terms.stratego;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermPrinter;
import org.spoofax.interpreter.terms.TermType;
import org.spoofax.terms.AbstractSimpleTerm;
import org.spoofax.terms.AbstractTermFactory;
import org.spoofax.terms.TermFactory;
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

    @Override public int getSubtermCount() {
        return 0;
    }

    @Override public IStrategoTerm getSubterm(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override public IStrategoTerm[] getAllSubterms() {
        return TermFactory.EMPTY_TERM_ARRAY;
    }

    @Override public List<IStrategoTerm> getSubterms() {
        return Collections.emptyList();
    }

    @Deprecated
    @Override public int getTermType() {
        return getType().getValue();
    }

    @Override public TermType getType() {
        return TermType.BLOB;
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
        return value.toString();
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

    @Override public Iterator<IStrategoTerm> iterator() {
        return new EmptyIterator<>();
    }
}
