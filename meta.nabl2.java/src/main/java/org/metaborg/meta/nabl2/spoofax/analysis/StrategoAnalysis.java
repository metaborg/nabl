package org.metaborg.meta.nabl2.spoofax.analysis;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.solver.Fresh;
import org.metaborg.meta.nabl2.solver.ISolution;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermPrinter;
import org.spoofax.terms.AbstractSimpleTerm;
import org.spoofax.terms.AbstractTermFactory;
import org.spoofax.terms.util.EmptyIterator;

public class StrategoAnalysis extends AbstractSimpleTerm implements IStrategoTerm, IScopeGraphUnit {
    private static final long serialVersionUID = 1L;

    private final IScopeGraphUnit unit;

    public StrategoAnalysis(IScopeGraphUnit unit) {
        this.unit = unit;
    }

    // IScopeGraphUnit delegation

    public String resource() {
        return unit.resource();
    }

    public Set<IConstraint> constraints() {
        return unit.constraints();
    }

    public Optional<ISolution> solution() {
        return unit.solution();
    }

    public Optional<CustomSolution> customSolution() {
        return unit.customSolution();
    }

    public Fresh fresh() {
        return unit.fresh();
    }

    public boolean isPrimary() {
        return unit.isPrimary();
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

    @Override public int getStorageType() {
        return IStrategoTerm.IMMUTABLE;
    }

    @Override public IStrategoList getAnnotations() {
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
        return "Analysis(" + resource() + ")";
    }

}