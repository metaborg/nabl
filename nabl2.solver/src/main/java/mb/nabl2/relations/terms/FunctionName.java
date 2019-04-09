package mb.nabl2.relations.terms;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import mb.nabl2.relations.IFunctionName;
import mb.nabl2.relations.terms.RelationName.NamedRelation;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

public abstract class FunctionName extends AbstractApplTerm implements IFunctionName, IApplTerm {

    public enum RelationFunctions {
        LUB, GLB;

        public String of(String relation) {
            return relation.isEmpty() ? name() : (relation + "." + name());
        }

    }


    @Value.Immutable
    @Serial.Version(value = 42L)
    static abstract class NamedFunction extends FunctionName {

        private static final String OP = "Function";

        @Value.Parameter public abstract String getName();

        // IFunctionName implementation

        @Override public <T> T match(IFunctionName.Cases<T> cases) {
            return cases.caseNamed(getName());
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

        public static IMatcher<NamedFunction> matcher() {
            return M.preserveAttachments(M.appl1(OP, M.stringValue(), (t, name) -> ImmutableNamedFunction.of(name)));
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
    static abstract class ExtFunction extends FunctionName {

        private static final String OP = "ExtFunction";

        @Value.Parameter public abstract String getName();

        // IFunctionName implementation

        @Override public <T> T match(IFunctionName.Cases<T> cases) {
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

        public static IMatcher<ExtFunction> matcher() {
            return M.preserveAttachments(M.appl1(OP, M.stringValue(), (t, name) -> ImmutableExtFunction.of(name)));
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

    public static IMatcher<? extends FunctionName> matcher() {
        // @formatter:off
        return M.preserveAttachments(M.cases(
            NamedFunction.matcher(),
            ExtFunction.matcher(),
            M.appl1("Lub", NamedRelation.matcher(), (t, r) -> ImmutableNamedFunction.of(RelationFunctions.LUB.of(r.getName()))),
            M.appl1("Glb", NamedRelation.matcher(), (t, r) -> ImmutableNamedFunction.of(RelationFunctions.GLB.of(r.getName())))
        ));
        // @formatter:on
    }

}