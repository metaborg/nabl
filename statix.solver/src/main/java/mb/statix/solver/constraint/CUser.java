package mb.statix.solver.constraint;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;
import mb.statix.spec.IRule;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.spec.ModuleBoundary;

/**
 * Implementation for a user constraint (rule application).
 * 
 * <pre>ruleName(arguments)</pre>
 */
public class CUser implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final List<ITerm> args;

    private final @Nullable IConstraint cause;

    /**
     * Creates a new user constraint without a cause.
     * 
     * @param name
     *      the name of the rule to invoke
     * @param args
     *      the arguments
     */
    public CUser(String name, Iterable<? extends ITerm> args) {
        this(name, args, null);
    }

    /**
     * Creates a new user constraint with a cause.
     * 
     * @param name
     *      the name of the rule to invoke
     * @param args
     *      the arguments
     * @param cause
     *      the constraint that caused this constraint to be added
     */
    public CUser(String name, Iterable<? extends ITerm> args, @Nullable IConstraint cause) {
        this.name = name;
        this.args = ImmutableList.copyOf(args);
        this.cause = cause;
    }

    public String name() {
        return name;
    }

    public List<ITerm> args() {
        return args;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CUser withCause(@Nullable IConstraint cause) {
        return new CUser(name, args, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseUser(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseUser(this);
    }

    @Override public CUser apply(ISubstitution.Immutable subst) {
        return new CUser(name, subst.apply(args), cause);
    }
    
    @Override
    public Optional<MConstraintResult> solve(IMState state, MConstraintContext params)
            throws InterruptedException, Delay {
        final IDebugContext debug = params.debug();
        final List<IRule> rules = Lists.newLinkedList(state.spec().rules().get(name));
        final Log unsuccessfulLog = new Log();
        final Iterator<IRule> it = rules.iterator();
        while(it.hasNext()) {
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            final LazyDebugContext proxyDebug = new LazyDebugContext(debug);
            final IRule rawRule = it.next();
            final String ruleOrModb = (rawRule instanceof ModuleBoundary) ? "module boundary" : "rule";
            if(proxyDebug.isEnabled(Level.Info)) {
                proxyDebug.info("Try {} {}", ruleOrModb, rawRule.toString());
            }
            
            final Set<IConstraint> instantiatedBody;
            final Tuple2<Set<ITermVar>, Set<IConstraint>> appl;
            
            try {
                if((appl = rawRule.apply(args, state).orElse(null)) != null) {
                    instantiatedBody = appl._2();
                } else {
                    proxyDebug.info("{} rejected (mismatching arguments)", ruleOrModb);
                    unsuccessfulLog.absorb(proxyDebug.clear());
                    continue;
                }
            } catch(Delay d) {
                proxyDebug.info("{} delayed (unsolved guard constraint)", ruleOrModb);
                unsuccessfulLog.absorb(proxyDebug.clear());
                unsuccessfulLog.flush(debug);
                throw d;
            }
            
            proxyDebug.info("{} accepted", ruleOrModb);
            proxyDebug.commit();
            return Optional.of(MConstraintResult.ofConstraints(instantiatedBody));
        }
        debug.info("No rule applies");
        unsuccessfulLog.flush(debug);
        return Optional.empty();
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("(");
        sb.append(termToString.format(args));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}
