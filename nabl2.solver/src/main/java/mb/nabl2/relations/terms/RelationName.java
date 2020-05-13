package mb.nabl2.relations.terms;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import mb.nabl2.relations.IRelationName;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

public abstract class RelationName extends AbstractApplTerm implements IRelationName, IApplTerm {

    @Value.Immutable
    @Serial.Version(value = 42L)
    static abstract class ANamedRelation extends RelationName {

        private static final String OP0 = "DefaultRelation";
        private static final String OP1 = "Relation";

        @Value.Parameter public abstract String getName();

        // IRelationName implementation

        @Override public <T> T match(IRelationName.Cases<T> cases) {
            return cases.caseNamed(getName());
        }

        @Override public <T, E extends Throwable> T matchOrThrow(IRelationName.CheckedCases<T, E> cases) throws E {
            return cases.caseNamed(getName());
        }

        // IApplTerm implementation

        @Override protected ANamedRelation check() {
            return this;
        }

        @Value.Lazy @Override public String getOp() {
            return getName().isEmpty() ? OP0 : OP1;
        }

        @Value.Lazy @Override public List<ITerm> getArgs() {
            return getName().isEmpty() ? ImmutableList.of() : ImmutableList.of((ITerm) B.newString(getName()));
        }

        public static IMatcher<NamedRelation> matcher() {
            // @formatter:off
            return M.preserveAttachments(M.cases(
                M.appl0(OP0, (t) -> NamedRelation.of("")),
                M.appl1(OP1, M.stringValue(), (t, name) -> NamedRelation.of(name))
            ));
            // @formatter:on
        }

        // Object implementation

        @Override public boolean equals(Object other) {
            if(other == null) {
                return false;
            }
            if(other == this) {
                return true;
            }
            if(!(other instanceof NamedRelation)) {
                return super.equals(other);
            }
            final NamedRelation that = (NamedRelation) other;
            if(!getName().equals(that.getName())) {
                return false;
            }
            return true;
        }

        @Override public int hashCode() {
            return super.hashCode();
        }

        @Override public String toString() {
            return getName();
        }

    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    static abstract class AExtRelation extends RelationName {

        private static final String OP = "ExtRelation";

        @Value.Parameter public abstract String getName();

        // IRelationName implementation

        @Override public <T> T match(IRelationName.Cases<T> cases) {
            return cases.caseExt(getName());
        }

        @Override public <T, E extends Throwable> T matchOrThrow(IRelationName.CheckedCases<T, E> cases) throws E {
            return cases.caseExt(getName());
        }

        // IApplTerm implementation

        @Override protected AExtRelation check() {
            return this;
        }

        @Value.Lazy @Override public String getOp() {
            return OP;
        }

        @Value.Lazy @Override public List<ITerm> getArgs() {
            return ImmutableList.of((ITerm) B.newString(getName()));
        }

        public static IMatcher<ExtRelation> matcher() {
            return M.preserveAttachments(M.appl1(OP, M.stringValue(), (t, name) -> ExtRelation.of(name)));
        }

        // Object implementation

        @Override public boolean equals(Object other) {
            if(other == null) {
                return false;
            }
            if(other == this) {
                return true;
            }
            if(!(other instanceof ExtRelation)) {
                return super.equals(other);
            }
            final ExtRelation that = (ExtRelation) other;
            if(!getName().equals(that.getName())) {
                return false;
            }
            return true;
        }

        @Override public int hashCode() {
            return super.hashCode();
        }

        @Override public String toString() {
            return "`" + getName() + "`";
        }

    }

    public static IMatcher<? extends RelationName> matcher() {
        // @formatter:off
        return M.preserveAttachments(M.cases(
            NamedRelation.matcher(),
            ExtRelation.matcher()
        ));
        // @formatter:on
    }

}