package mb.statix.solver.constraint;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.ConstraintResult;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;
import mb.statix.spec.IRule;
import mb.statix.spec.Spec;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.MState;
import mb.statix.taico.spec.ModuleBoundary;

/**
 * Implementation for a user constraint (rule application).
 * 
 * <pre>ruleName(arguments)</pre>
 */
public class CUser implements IConstraint {

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

    @Override public Iterable<ITerm> terms() {
        return args;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CUser withCause(@Nullable IConstraint cause) {
        return new CUser(name, args, cause);
    }

    @Override public Collection<CriticalEdge> criticalEdges(Spec spec) {
        return spec.scopeExtensions().get(name).stream().map(il -> CriticalEdge.of(args.get(il._1()), il._2()))
                .collect(Collectors.toList());
    }

    @Override public CUser apply(ISubstitution.Immutable subst) {
        return new CUser(name, subst.apply(args), cause);
    }
    
    @Override
    public boolean canModifyState() {
        return true;
    }

    /**
     * @see IConstraint#solve
     * 
     * @throws InterruptedException
     *      If the current thread has been interrupted.
     * @throws Delay
     *      If the guard constraints on one of the rule candidates are not solved.
     */
    public Optional<ConstraintResult> solve(final State state, ConstraintContext params)
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
            if(proxyDebug.isEnabled(Level.Info)) {
                proxyDebug.info("Try rule {}", rawRule.toString());
            }
            final State instantiatedState;
            final Set<IConstraint> instantiatedBody;
            final Tuple3<State, Set<ITermVar>, Set<IConstraint>> appl;
            try {
                if((appl = rawRule.apply(args, state).orElse(null)) != null) {
                    instantiatedState = appl._1();
                    instantiatedBody = appl._3();
                } else {
                    proxyDebug.info("Rule rejected (mismatching arguments)");
                    unsuccessfulLog.absorb(proxyDebug.clear());
                    continue;
                }
            } catch(Delay d) {
                proxyDebug.info("Rule delayed (unsolved guard constraint)");
                unsuccessfulLog.absorb(proxyDebug.clear());
                unsuccessfulLog.flush(debug);
                throw d;
            }
            
            proxyDebug.info("Rule accepted");
            proxyDebug.commit();
            return Optional.of(ConstraintResult.ofConstraints(instantiatedState, instantiatedBody));
        }
        debug.info("No rule applies");
        unsuccessfulLog.flush(debug);
        return Optional.empty();
    }
    
    @Override
    public Optional<MConstraintResult> solveMutable(MState state, MConstraintContext params)
            throws InterruptedException, Delay {
        if (name.startsWith("modbound_")) {
            CModule modc = new CModule(name, args);
            //TODO test directly using modc.solveMutable(state, params);
            return Optional.of(MConstraintResult.ofConstraints(state, modc));
        }
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
                //TODO IMPORTANT In the module boundary, I used to have a state copy here. Is this required?
                if (rawRule instanceof ModuleBoundary) System.err.println("Module boundary encountered, is state copy of original required?");
                MState copyState = state.copy();
                if((appl = rawRule.apply(args, copyState).orElse(null)) != null) {
                    instantiatedBody = appl._2();
                    state = copyState;
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
            return Optional.of(MConstraintResult.ofConstraints(state, instantiatedBody));
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
