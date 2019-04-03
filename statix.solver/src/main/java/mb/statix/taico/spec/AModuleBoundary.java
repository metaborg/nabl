package mb.statix.taico.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.TermFormatter;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.spec.ARule;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleString;
import mb.statix.taico.scopegraph.IOwnableScope;
import mb.statix.taico.scopegraph.OwnableScope;
import mb.statix.taico.solver.MState;

/**
 * Class which describes a statix rule.
 * 
 * <pre>modbound ruleName(paramVars) | $[moduleName]:- {bodyVars} constraints.</pre>
 */
@Value.Immutable
public abstract class AModuleBoundary extends ARule {

    @Value.Parameter public abstract String name();

    @Value.Parameter public abstract List<Pattern> params();
    
    @Value.Parameter public abstract ModuleString moduleString();

    @Value.Parameter public abstract Set<ITermVar> bodyVars();

    @Value.Parameter public abstract List<IConstraint> body();

    @Override
    public ModuleBoundary apply(ISubstitution.Immutable subst) {
        final ISubstitution.Immutable bodySubst = subst.removeAll(paramVars()).removeAll(bodyVars());
        final ModuleString newModuleString = moduleString().apply(subst);
        final List<IConstraint> newBody = body().stream().map(c -> c.apply(bodySubst)).collect(Collectors.toList());
        return ModuleBoundary.of(name(), params(), newModuleString, bodyVars(), newBody);
    }
    
    @Override
    public Optional<Tuple2<Set<ITermVar>, Set<IConstraint>>> apply(List<ITerm> args, MState state) throws Delay {
        List<ITerm> newArgs = groundArguments(args, state.unifier());
        
        final ISubstitution.Transient subst;
        final Optional<Immutable> matchResult = P.match(params(), newArgs, state.unifier()).matchOrThrow(r -> r, vars -> {
            throw Delay.ofVars(vars);
        });
        if((subst = matchResult.map(u -> u.melt()).orElse(null)) == null) {
            return Optional.empty();
        }
        
        //We don't always want to statically store the child relation. We want to base this on the current owner.
        Set<IOwnableScope> canExtend = new HashSet<>();
        for (ITerm term : newArgs) {
            OwnableScope scope = OwnableScope.ownableMatcher(state.manager()::getModule).match(term).orElse(null);
            if (scope != null) canExtend.add(scope);
        }
        
        IModule child = state.owner().createChild(canExtend);
        
        MState childState = new MState(state.manager(), state.coordinator(), child, state.spec());
        
        final ImmutableSet.Builder<ITermVar> freshBodyVars = ImmutableSet.builder();
        for(ITermVar var : bodyVars()) {
            final ITermVar term = childState.freshVar(var.getName());
            subst.put(var, term);
            freshBodyVars.add(term);
        }
        final ISubstitution.Immutable isubst = subst.freeze();
        final Set<IConstraint> newBody = body().stream().map(c -> c.apply(isubst)).collect(Collectors.toSet());
        
        Set<ITermVar> freshVars = freshBodyVars.build();
        //TODO IMPORTANT Fix the isRigid and isClosed to their correct forms (check ownership and delegate)
        state.solver().childSolver(childState, newBody, state.solver().isRigid(), state.solver().isClosed());
        
        //We return an empty set since we don't want to add constraints to the current solver, as a child is solving it.
        return Optional.of(ImmutableTuple2.of(freshVars, Collections.emptySet()));
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
        //TODO IMPORTANT This function does not seem to actually ground the arguments.
        for (ITerm term : args) {
            if (!unifier.isGround(term)) {
                //TODO IMPORTANT Is this correct? How about a term where some of it's innards are unknown, but not all of them? (The delay waits on all vars, but some might be known)
                throw Delay.ofVars(unifier.getVars(term));
            }
        }
        
        final List<ITerm> newArgs = new ArrayList<>();
        for (ITerm term : args) {
            if (term instanceof ITermVar) {
                //TODO IMPOTANT try catch?
                ITerm actual = unifier.findRecursive(term);
                newArgs.add(actual);
            } else {
                newArgs.add(term);
            }
        }
        
        return args;
    }
    
    /**
     * Formats this rule where constraints are formatted with the given TermFormatter.
     * 
     * <pre>modbound name(params) | $[moduleName] :- {bodyVars} constraints.</pre>
     * 
     * @param termToString
     *      the term formatter to format constraints with
     * 
     * @return
     *      the string
     */
    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("modbound ").append(name()).append("(").append(params()).append(")");
        sb.append(" | ").append(moduleString());
        sb.append(" :- ");
        if(!bodyVars().isEmpty()) {
            sb.append("{").append(bodyVars()).append("} ");
        }
        sb.append(IConstraint.toString(body(), termToString.removeAll(bodyVars())));
        return sb.toString();
    }

}