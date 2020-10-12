package mb.nabl2.scopegraph.terms;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.IOccurrenceIndex;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.stratego.TermIndex;

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

    @Override public boolean isGround() {
        return term.isGround();
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return term.getVars();
    }

    @Override public IAttachments getAttachments() {
        return term.getAttachments();
    }

    @Override public OccurrenceIndex withAttachments(IAttachments value) {
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

    @Override public boolean equals(Object other, boolean compareAttachments) {
        if(this == other)
            return true;
        if(!(other instanceof ITerm))
            return false;
        // @formatter:off
        return equals(other)
            && (!compareAttachments || Objects.equals(this.getAttachments(), ((ITerm)other).getAttachments()));
        // @formatter:on
    }

    @Override public int hashCode() {
        return term.hashCode();
    }

    @Override public String toString() {
        return term.toString();
    }

    // static

    public static IMatcher<OccurrenceIndex> matcher() {
        // @formatter:off
        return M.preserveAttachments(M.cases(
            M.term(TermIndex.matcher(), (t, i) -> OccurrenceIndex.of(i)),
            M.term(Scope.matcher(), (t, s) -> OccurrenceIndex.of(s))
        ));
        // @formatter:on

    }

    public static OccurrenceIndex of(TermIndex i) {
        return new OccurrenceIndex(i.getResource(), i);
    }

    public static OccurrenceIndex of(Scope s) {
        return new OccurrenceIndex(s.getResource(), s);
    }

}
