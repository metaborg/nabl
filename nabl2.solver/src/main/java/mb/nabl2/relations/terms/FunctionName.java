package mb.nabl2.relations.terms;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.ImList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.scopegraph.relations.IFunctionName;

public abstract class FunctionName extends AbstractApplTerm implements IFunctionName, IApplTerm {

    public enum RelationFunctions {
        LUB, GLB;

        public String of(String relation) {
            return relation.isEmpty() ? name() : (relation + "." + name());
        }

    }


    @Value.Immutable
    @Serial.Version(value = 42L)
    static abstract class ANamedFunction extends FunctionName {

        private static final String OP = "Function";

        @Value.Parameter public abstract String getName();

        // IFunctionName implementation

        @Override public <T> T match(IFunctionName.Cases<T> cases) {
            return cases.caseNamed(getName());
        }

        @Override public <T, E extends Throwable> T matchOrThrow(IFunctionName.CheckedCases<T, E> cases) throws E {
            return cases.caseNamed(getName());
        }

        // IApplTerm implementation

        @Override protected ANamedFunction check() {
            return this;
        }

        @Value.Lazy @Override public String getOp() {
            return OP;
        }

        @Value.Lazy @Override public ImList.Immutable<ITerm> getArgs() {
            return ImList.Immutable.of((ITerm) B.newString(getName()));
        }

        public static IMatcher<NamedFunction> matcher() {
            return M.preserveAttachments(M.appl1(OP, M.stringValue(), (t, name) -> NamedFunction.of(name)));
        }

        // Object implementation

        @Override public boolean equals(Object other) {
            if(other == null) {
                return false;
            }
            if(other == this) {
                return true;
            }
            if(!(other instanceof NamedFunction)) {
                return super.equals(other);
            }
            final NamedFunction that = (NamedFunction) other;
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
    static abstract class AExtFunction extends FunctionName {

        private static final String OP = "ExtFunction";

        @Value.Parameter public abstract String getName();

        // IFunctionName implementation

        @Override public <T> T match(IFunctionName.Cases<T> cases) {
            return cases.caseExt(getName());
        }

        @Override public <T, E extends Throwable> T matchOrThrow(IFunctionName.CheckedCases<T, E> cases) throws E {
            return cases.caseExt(getName());
        }

        // IApplTerm implementation

        @Override protected AExtFunction check() {
            return this;
        }

        @Value.Lazy @Override public String getOp() {
            return OP;
        }

        @Value.Lazy @Override public ImList.Immutable<ITerm> getArgs() {
            return ImList.Immutable.of((ITerm) B.newString(getName()));
        }

        public static IMatcher<ExtFunction> matcher() {
            return M.preserveAttachments(M.appl1(OP, M.stringValue(), (t, name) -> ExtFunction.of(name)));
        }

        // Object implementation

        @Override public boolean equals(Object other) {
            if(other == null) {
                return false;
            }
            if(other == this) {
                return true;
            }
            if(!(other instanceof ExtFunction)) {
                return super.equals(other);
            }
            final ExtFunction that = (ExtFunction) other;
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

    public static IMatcher<? extends FunctionName> matcher() {
        // @formatter:off
        return M.preserveAttachments(M.cases(
            NamedFunction.matcher(),
            ExtFunction.matcher(),
            M.appl1("Lub", NamedRelation.matcher(), (t, r) -> NamedFunction.of(RelationFunctions.LUB.of(r.getName()))),
            M.appl1("Glb", NamedRelation.matcher(), (t, r) -> NamedFunction.of(RelationFunctions.GLB.of(r.getName())))
        ));
        // @formatter:on
    }

}