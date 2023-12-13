package mb.statix.constraints;

import java.util.Optional;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;
import mb.statix.solver.query.QueryProject;

public abstract class AResolveQuery implements IResolveQuery {

    protected final QueryFilter filter;
    protected final QueryMin min;
    protected final QueryProject project;
    protected final ITerm scopeTerm;
    protected final ITerm resultTerm;

    protected final @Nullable IConstraint cause;
    protected final @Nullable IMessage message;

    protected AResolveQuery(
            QueryFilter filter,
            QueryMin min,
            QueryProject project,
            ITerm scopeTerm,
            ITerm resultTerm,
            @Nullable IConstraint cause,
            @Nullable IMessage message
    ) {
        this.filter = filter;
        this.min = min;
        this.project = project;
        this.scopeTerm = scopeTerm;
        this.resultTerm = resultTerm;
        this.cause = cause;
        this.message = message;
    }

    public QueryFilter filter() {
        return filter;
    }

    public QueryMin min() {
        return min;
    }

    public QueryProject project() {
        return project;
    }

    public ITerm scopeTerm() {
        return scopeTerm;
    }

    public ITerm resultTerm() {
        return resultTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public Optional<IMessage> message() {
        return Optional.ofNullable(message);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        final Set.Transient<ITermVar> vars = CapsuleUtil.transientSet();
        vars.__insertAll(filter.getVars());
        vars.__insertAll(min.getVars());
        vars.__insertAll(scopeTerm.getVars());
        vars.__insertAll(resultTerm.getVars());
        return vars.freeze();
    }

    @Override public Set.Immutable<ITermVar> freeVars() {
        Set.Transient<ITermVar> freeVars = CapsuleUtil.transientSet();
        doVisitFreeVars(freeVars::__insert);
        return freeVars.freeze();
    }

    @Override public void visitFreeVars(Action1<ITermVar> onFreeVar) {
        doVisitFreeVars(onFreeVar);
    }

    private void doVisitFreeVars(Action1<ITermVar> onFreeVar) {
        scopeTerm.getVars().forEach(onFreeVar::apply);
        filter.getDataWF().visitFreeVars(onFreeVar);
        min.getDataEquiv().visitFreeVars(onFreeVar);
        resultTerm.getVars().forEach(onFreeVar::apply);
        if (message != null) {
            message.visitVars(onFreeVar);
        }
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}
