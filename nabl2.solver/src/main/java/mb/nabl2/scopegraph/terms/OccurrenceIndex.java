package mb.nabl2.scopegraph.terms;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.Serializable;
import java.util.List;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.Multiset;

import mb.nabl2.scopegraph.IOccurrenceIndex;
import mb.nabl2.stratego.TermIndex;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

public class OccurrenceIndex implements IOccurrenceIndex, IApplTerm, Serializable {

    private static final long serialVersionUID = 42L;

    private final String resource;
    private final IApplTerm term;

    public OccurrenceIndex(String resource, IApplTerm term) {
        this.resource = resource;
        this.term = term;
    }

    @Override public String getResource() {
        return resource;
    }

    // IApplTerm

    public boolean isGround() {
        return term.isGround();
    }

    @Override public boolean isLocked() {
        return term.isLocked();
    }

    @Override public OccurrenceIndex withLocked(boolean locked) {
        return new OccurrenceIndex(resource, term.withLocked(locked));
    }

    @Override public Multiset<ITermVar> getVars() {
        return term.getVars();
    }

    @Override public ImmutableClassToInstanceMap<Object> getAttachments() {
        return term.getAttachments();
    }

    @Override public OccurrenceIndex withAttachments(ImmutableClassToInstanceMap<Object> value) {
        return new OccurrenceIndex(resource, term.withAttachments(value));
    }

    @Override public String getOp() {
        return term.getOp();
    }

    @Override public int getArity() {
        return term.getArity();
    }

    @Override public List<ITerm> getArgs() {
        return term.getArgs();
    }

    @Override public <T> T match(Cases<T> cases) {
        return term.match(cases);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return term.matchOrThrow(cases);
    }

    // Object

    @Override public boolean equals(Object other) {
        return term.equals(other);
    }

    @Override public int hashCode() {
        return term.hashCode();
    }

    @Override public String toString() {
        return term.toString();
    }

    // static

    public static IMatcher<OccurrenceIndex> matcher() {
        return M.preserveAttachments(M.cases(
        // @formatter:off
            M.term(TermIndex.matcher(), (t, i) -> new OccurrenceIndex(i.getResource(), i)),
            M.term(Scope.matcher(), (t, s) -> new OccurrenceIndex(s.getResource(), s))
            // @formatter:on
        ));

    }

}