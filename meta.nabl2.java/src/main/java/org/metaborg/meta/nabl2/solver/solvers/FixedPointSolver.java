package org.metaborg.meta.nabl2.solver.solvers;

import java.util.Iterator;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.solver.ISolver;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.ImmutableSolveResult;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.meta.nabl2.solver.properties.IConstraintSetProperty;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class FixedPointSolver {

    private final ICancel cancel;
    private final IProgress progress;

    private final ISolver<IConstraint, ?> component;
    private final java.util.Set<IConstraintSetProperty> properties;

    public FixedPointSolver(ICancel cancel, IProgress progress, ISolver<IConstraint, ?> component,
            Iterable<? extends IConstraintSetProperty> properties) {
        this(cancel, progress, component, Sets.newHashSet(properties));
    }

    public FixedPointSolver(ICancel cancel, IProgress progress, ISolver<IConstraint, ?> component,
            Set<IConstraintSetProperty> properties) {
        this.cancel = cancel;
        this.progress = progress;
        this.component = component;
        this.properties = properties;
    }

    public SolveResult solve(Iterable<? extends IConstraint> initialConstraints) throws InterruptedException {
        propertiesAddAll(initialConstraints);

        final IMessages.Transient messages = Messages.Transient.of();
        final Multimap<String, String> strongDependencies = HashMultimap.create();
        final Multimap<String, String> weakDependencies = HashMultimap.create();

        final Set<IConstraint> constraints = Sets.newHashSet(initialConstraints);
        boolean progress;
        do {
            progress = false;
            final Set<IConstraint> newConstraints = Sets.newHashSet();
            final Iterator<IConstraint> it = constraints.iterator();
            while(it.hasNext()) {
                cancel.throwIfCancelled();
                final IConstraint constraint = it.next();
                final SolveResult result;
                propertiesRemove(constraint); // property only on other constraints
                if((result = component.solve(constraint).orElse(null)) != null) {
                    messages.addAll(result.messages());

                    strongDependencies.putAll(result.strongDependencies());
                    weakDependencies.putAll(result.weakDependencies());

                    propertiesAddAll(result.constraints());
                    newConstraints.addAll(result.constraints());


                    propertiesUpdate(result.unifiedVars());
                    it.remove();

                    component.update();

                    this.progress.work(1);
                    progress |= true;
                } else {
                    propertiesAdd(constraint);
                }
            }
            constraints.addAll(newConstraints);
        } while(progress);

        return ImmutableSolveResult.builder()
                // @formatter:off
                .messages(messages.freeze())
                .strongDependencies(strongDependencies)
                .weakDependencies(weakDependencies)
                .constraints(constraints)
                // @formatter:on
                .build();
    }

    private void propertiesAddAll(Iterable<? extends IConstraint> constraints) {
        for(IConstraintSetProperty property : properties) {
            property.addAll(constraints);
        }
    }

    private void propertiesAdd(IConstraint constraint) {
        for(IConstraintSetProperty property : properties) {
            property.add(constraint);
        }
    }

    private void propertiesRemove(IConstraint constraint) {
        for(IConstraintSetProperty property : properties) {
            property.remove(constraint);
        }
    }

    private void propertiesUpdate(Set<ITermVar> vars) {
        for(ITermVar var : vars) {
            for(IConstraintSetProperty property : properties) {
                property.update(var);
            }
        }
    }

}