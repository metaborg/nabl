package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action2;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.u.IUnifier;

public final class ApplPattern extends Pattern {
    private static final long serialVersionUID = 1L;

    private final String op;
    private final List<Pattern> args;

    ApplPattern(String op, Iterable<? extends Pattern> args, IAttachments attachments) {
        super(attachments);
        this.op = op;
        this.args = ImmutableList.copyOf(args);
    }

    public String getOp() {
        return op;
    }

    public List<Pattern> getArgs() {
        return args;
    }

    @Override public Set<ITermVar> getVars() {
        Set.Transient<ITermVar> vars = CapsuleUtil.transientSet();
        for(Pattern arg : args) {
            vars.__insertAll(arg.getVars());
        }
        return vars.freeze();
    }

    @Override public boolean isConstructed() {
        return true;
    }

    @Override protected boolean matchTerm(ITerm term, ISubstitution.Transient subst, IUnifier.Immutable unifier,
            Eqs eqs) {
        // @formatter:off
        return unifier.findTerm(term).match(Terms.<Boolean>cases()
            .appl(applTerm -> {
                if(applTerm.getArity() == this.args.size() && applTerm.getOp().equals(op)) {
                    return matchTerms(args, applTerm.getArgs(), subst, unifier, eqs);
                } else {
                    return false;
                }
            }).var(v -> {
                eqs.add(v, this);
                return true;
            }).otherwise(t -> {
                return false;
            })
        );
        // @formatter:on
    }

    @Override public Pattern apply(IRenaming subst) {
        final ImmutableList.Builder<Pattern> newArgs = ImmutableList.builderWithExpectedSize(args.size());
        for(Pattern arg : args) {
            newArgs.add(arg.apply(subst));
        }
        return new ApplPattern(op, newArgs.build(), getAttachments());
    }

    @Override public ApplPattern eliminateWld(Function0<ITermVar> fresh) {
        final ImmutableList.Builder<Pattern> newArgs = ImmutableList.builderWithExpectedSize(args.size());
        for(Pattern arg : args) {
            newArgs.add(arg.eliminateWld(fresh));
        }
        return new ApplPattern(op, newArgs.build(), getAttachments());
    }

    @Override protected ITerm asTerm(Action2<ITermVar, ITerm> equalities,
            Function1<Optional<ITermVar>, ITermVar> fresh) {
        final ImmutableList.Builder<ITerm> newArgs = ImmutableList.builderWithExpectedSize(args.size());
        for(Pattern arg : args) {
            newArgs.add(arg.asTerm(equalities, fresh));
        }
        return B.newAppl(op, newArgs.build(), getAttachments());
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(op);
        sb.append("(").append(args.stream().map(Object::toString).collect(Collectors.joining(",", "", ""))).append(")");
        return sb.toString();
    }

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        ApplPattern that = (ApplPattern) o;
        return Objects.equals(op, that.op) && Objects.equals(args, that.args);
    }

    @Override public int hashCode() {
        return Objects.hash(op, args);
    }
}
