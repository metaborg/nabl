package mb.nabl2.terms.build;

import java.util.Objects;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

public abstract class AbstractApplTerm extends AbstractTerm implements IApplTerm {

    private static final byte IS_GROUND_SET = 1;
    private static final byte IS_GROUND_FLAG = 2;

    @Value.Check protected abstract IApplTerm check(); // return type must match the class, so cannot be
                                                       // implemented in a superclass, but must be implemented
                                                       // in every subclass.

    @Override public abstract String getOp();

    @Override public int getArity() {
        return getArgs().size();
    }


    private volatile byte isGround;

    @Override public boolean isGround() {
        byte result = isGround;
        if((result & IS_GROUND_SET) == 0) {
            boolean ground = true;
            for(ITerm arg : getArgs()) {
                ground &= arg.isGround();
            }
            result = (byte) (IS_GROUND_SET | (ground ? IS_GROUND_FLAG : 0));
            isGround = result;
        }
        return (result & IS_GROUND_FLAG) != 0;
    }


    @Override public Set.Immutable<ITermVar> getVars() {
        if(isGround()) {
            return CapsuleUtil.immutableSet();
        }
        final Set.Transient<ITermVar> vars = CapsuleUtil.transientSet();
        visitVars(vars::__insert);
        return vars.freeze();
    }

    @Override public void visitVars(Action1<ITermVar> onVar) {
        if(isGround()) {
            return;
        }
        for(ITerm arg : getArgs()) {
            arg.visitVars(onVar);
        }
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseAppl(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseAppl(this);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(getOp(), getArgs());
            hashCode = result;
        }
        return result;
    }

    @Override public boolean equals(Object other) {
        if(this == other)
            return true;
        if(!(other instanceof IApplTerm))
            return false;
        IApplTerm that = (IApplTerm) other;
        if(this.hashCode() != that.hashCode())
            return false;
        // @formatter:off
        return Objects.equals(this.getOp(), that.getOp())
            && Objects.equals(this.getArity(), that.getArity())
            && Objects.equals(this.getArgs(), that.getArgs());
        // @formatter:on
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getOp());
        sb.append("(").append(getArgs().stream().map(Object::toString).collect(Collectors.joining(",", "", "")))
                .append(")");
        return sb.toString();
    }

}
