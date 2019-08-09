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
import mb.statix.taico.module.ModulePaths;
import mb.statix.taico.solver.Context;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.state.IMState;
import mb.statix.taico.spec.ModuleBoundary;
import mb.statix.taico.util.Scopes;

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
    private boolean skipModuleBoundary;

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
        this.skipModuleBoundary = original.skipModuleBoundary;
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
                if (rawRule instanceof ModuleBoundary && !skipModuleBoundary) {
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

                    createChild(state, rule, newArgs, appl);

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
     * Creates a new child module from the given module boundary and result.
     * 
     * @param state
     *      the state of the parent module
     * @param rule
     *      the module boundary
     * @param newArgs
     *      the arguments passed to the module boundary
     * @param appl
     *      the application of the rule
     * 
     * @return
     *      the created child module
     */
    private IModule createChild(IMState state, ModuleBoundary rule, List<ITerm> newArgs,
            Tuple2<Immutable, IConstraint> appl) {
        //Determine the scopes that the child can extend (order matters)
        List<Scope> canExtend = Scopes.getScopeTerms(newArgs);
        
        //The name of the module has to be built
        String modName = rule.moduleString().build(appl._1());
        
        ModuleSolver oldChildSolver, newChildSolver;
        
        //If an old child module should be reused, it will be returned here.
        IModule child = state.owner().getChildIfAllowed(modName, canExtend, skipModuleBoundary(newArgs));
        if (child != null) {
            //TODO This code has never been tested
            
            //Reuse an old child if it is clean
            System.err.println("Reusing old child module: " + child);
            
            MSolverResult result = Context.context().getOldContext().getResult(child);
            IMState childState = child.getCurrentState();
            oldChildSolver = childState.solver();
            newChildSolver = state.solver().noopSolver(childState, result);
        } else {
            //Lookup the old child solver before it is replaced by the call to createChild.
            String childId = ModulePaths.build(state.owner().getId(), modName);
            oldChildSolver = Context.context().getSolver(childId);
            
            child = state.owner().createChild(modName, canExtend, skipModuleBoundary(newArgs), false);
            newChildSolver = state.solver().childSolver(child.getCurrentState(), appl._2());
        }
        
        //If there was an old child solver, replace it with the new one
        if (oldChildSolver != null) oldChildSolver.cleanUpForReplacement(newChildSolver);
        
        //Make the child module available to the rest of the world
        state.owner().addChild(child);
        return child;
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

    /**
     * @param args
     *      the arguments to the user constraint
     * 
     * @return
     *      a new CUser constraint that will not execute as a module boundary even if it is one
     */
    private CUser skipModuleBoundary(List<ITerm> args) {
        CUser tbr = new CUser(name(), args);
        tbr.skipModuleBoundary = true;
        return tbr;
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
