package org.metaborg.meta.nabl2.relations.terms;

import static org.metaborg.meta.nabl2.terms.build.TermBuild.B;
import static org.metaborg.meta.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.build.AbstractApplTerm;
import org.metaborg.meta.nabl2.terms.matching.TermMatch.IMatcher;

import com.google.common.collect.ImmutableList;

public abstract class RelationName extends AbstractApplTerm implements IRelationName, IApplTerm {

    @Value.Immutable
    @Serial.Version(value = 42L)
    static abstract class NamedRelation extends RelationName {

        private static final String OP0 = "DefaultRelation";
        private static final String OP1 = "Relation";

        @Value.Parameter public abstract String getName();

        // IRelationName implementation

        public <T> T match(IRelationName.Cases<T> cases) {
            return cases.caseNamed(getName());
        }

        // IApplTerm implementation

        @Override protected IApplTerm check() {
            return this;
        }

        @Value.Lazy @Override public String getOp() {
            return getName().isEmpty() ? OP0 : OP1;
        }

        @Value.Lazy @Override public List<ITerm> getArgs() {
            return getName().isEmpty() ? ImmutableList.of() : ImmutableList.of((ITerm) B.newString(getName()));
        }

        public static IMatcher<NamedRelation> matcher() {
            return M.preserveAttachments(M.cases(
            // @formatter:off
                M.appl0(OP0, (t) -> ImmutableNamedRelation.of("")),
                M.appl1(OP1, M.stringValue(), (t, name) -> ImmutableNamedRelation.of(name))
                // @formatter:on
            ));
        }

        // Object implementation

        @Override public boolean equals(Object other) {
            return super.equals(other);
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
    static abstract class ExtRelation extends RelationName {

        private static final String OP = "ExtRelation";

        @Value.Parameter public abstract String getName();

        // IRelationName implementation

        public <T> T match(IRelationName.Cases<T> cases) {
            return cases.caseExt(getName());
        }

        // IApplTerm implementation

        @Override protected IApplTerm check() {
            return this;
        }

        @Value.Lazy @Override public String getOp() {
            return OP;
        }

        @Value.Lazy @Override public List<ITerm> getArgs() {
            return ImmutableList.of((ITerm) B.newString(getName()));
        }

        public static IMatcher<ExtRelation> matcher() {
            return M.preserveAttachments(M.appl1(OP, M.stringValue(), (t, name) -> ImmutableExtRelation.of(name)));
        }

        // Object implementation

        @Override public boolean equals(Object other) {
            return super.equals(other);
        }

        @Override public int hashCode() {
            return super.hashCode();
        }

        @Override public String toString() {
            return "`" + getName() + "`";
        }

    }

    public static IMatcher<? extends RelationName> matcher() {
        return M.preserveAttachments(M.cases(
            // @formatter:off
            NamedRelation.matcher(),
            ExtRelation.matcher()
            // @formatter:on
        ));
    }

}