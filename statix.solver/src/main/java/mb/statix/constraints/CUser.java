package mb.statix.constraints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;
import mb.statix.spec.IRule;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.Context;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.state.IMState;
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
    
    private final @Nullable IRule appliedRule;

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
        this.appliedRule = null;
    }
    
    /**
     * Creates a new CUser constraint with the given applied rule.
     * 
     * @param original
     *      the original CUser
     * @param rule
     *      the rule that was applied
     */
    private CUser(CUser original, IRule rule) {
        this.name = original.name;
        this.args = original.args;
        this.cause = original.cause;
        this.appliedRule = rule;
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
    
    /**
     * Creates a new CUser with the given rule as the rule that was selected.
     * This information is used by tracing rule applications for scope identity.
     * 
     * @param rule
     *      the rule that was applied
     * 
     * @return
     */
    public CUser withAppliedRule(IRule rule) {
        return new CUser(this, rule);
    }
    
    /**
     * The rule that was selected and applied by this user constraint.
     * 
     * @return
     *      the rule that was applied, or null if no rule was applied
     */
    public @Nullable IRule getAppliedRule() {
        return appliedRule;
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
            
            final IConstraint instantiatedBody;
            try {
                if (rawRule instanceof ModuleBoundary) {
                    ModuleBoundary rule = (ModuleBoundary) rawRule;
                    if (state.solver().isSeparateSolver()) {
                        throw new UnsupportedOperationException("Separate solvers (entailment) cannot cross module boundaries. (At " + this + ")");
                    }
                    List<ITerm> newArgs = groundArguments(args, state.unifier());
                    
                    final Tuple2<Immutable, IConstraint> appl;
                    if((appl = rawRule.apply(newArgs, state.unifier()).orElse(null)) == null) {
                        proxyDebug.info("{} rejected (mismatching arguments)", ruleOrModb);
                        unsuccessfulLog.absorb(proxyDebug.clear());
                        continue;
                    }                     
                    
                    //We don't always want to statically store the child relation. We want to base this on the current owner.
                    List<Scope> canExtend = new ArrayList<>();
                    for (ITerm term : newArgs) {
                        Scope scope = Scope.matcher().match(term).orElse(null);
                        if (scope != null) canExtend.add(scope);
                    }
                    
                    String modName = rule.moduleString().build(appl._1());
                    
                    IModule child = state.owner().getChildIfClean(modName, canExtend, new CUser(name(), newArgs));
                    if (child != null) {
                        System.err.println("Reusing old child: " + child);
                        //Reuse an old child if it is clean (1), and add the child to the scope graph of the state owner (2)
                        MSolverResult result = Context.context().getOldContext().get().getResult(child);
                        ModuleSolver oldChildSolver = child.getCurrentState().solver();
                        state.solver().noopSolver(child.getCurrentState(), result);
                        if (oldChildSolver != null) {
                            oldChildSolver.cleanUpForReplacement();
                        }
                        state.owner().addChild(child);
                    } else {
                        //Create the child module + state (1), create the state (2), create the child solver (3) and add the child to the scope graph of the state owner (4)
                        //This needs to happen in this precise ordering to prevent concurrency issues. The third call makes the new child module
                        //available to the rest of the world.
                        child = state.owner().createChild(modName, canExtend, new CUser(name(), newArgs), false);
                        state.solver().childSolver(child.getCurrentState(), appl._2());
                        state.owner().addChild(child);
                    }

                    proxyDebug.info("{} accepted", ruleOrModb);
                    proxyDebug.commit();
                    return Optional.of(MConstraintResult.of());
                } else {
                    if((instantiatedBody = rawRule.apply(args, state.unifier()).map(Tuple2::_2).orElse(null)) == null) {
                        proxyDebug.info("{} rejected (mismatching arguments)", ruleOrModb);
                        unsuccessfulLog.absorb(proxyDebug.clear());
                        continue;
                    }   
                    
                    proxyDebug.info("{} accepted", ruleOrModb);
                    proxyDebug.commit();
                    
                    //Add rule tracking
                    IConstraint tbr = instantiatedBody.withCause(this.withAppliedRule(rawRule));
                    return Optional.of(MConstraintResult.ofConstraints(tbr));
                }
            } catch(Delay d) {
                proxyDebug.info("{} delayed (unsolved guard constraint)", ruleOrModb);
                unsuccessfulLog.absorb(proxyDebug.clear());
                unsuccessfulLog.flush(debug);
                throw d;
            }

        }
        debug.info("No rule applies");
        unsuccessfulLog.flush(debug);
        return Optional.empty();
    }

    /**
     * If any of the arguments are not ground, this method throws a delay exception.
     * Otherwise, it recursively and eagerly evaluates each argument so it can be passed to the
     * new module.
     * 
     * @param args
     *      the arguments to make ground
     * @param unifier
     *      the unifier to use
     * 
     * @return
     *      the list of arguments, recursively evaluated
     * 
     * @throws Delay
     *      If one of the arguments is not ground.
     */
    private List<ITerm> groundArguments(List<ITerm> args, final IUnifier.Immutable unifier) throws Delay {
        for (ITerm term : args) {
            if (!unifier.isGround(term)) {
                //TODO IMPORTANT Is this correct? How about a term where some of it's innards are unknown, but not all of them? (The delay waits on all vars, but some might be known)
                throw Delay.ofVars(unifier.getVars(term));
            }
        }
        
        final List<ITerm> newArgs = new ArrayList<>();
        for (ITerm term : args) {
            if (term instanceof ITermVar) {
                ITerm actual = unifier.findRecursive(term);
                newArgs.add(actual);
            } else {
                newArgs.add(term);
            }
        }
        
        return newArgs;
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
