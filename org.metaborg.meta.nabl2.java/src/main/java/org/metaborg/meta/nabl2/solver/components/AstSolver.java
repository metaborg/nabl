package org.metaborg.meta.nabl2.solver.components;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.ast.CAstProperty;
import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint;
import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.ast.ImmutableCAstProperty;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.ImmutableMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageKind;
import org.metaborg.meta.nabl2.solver.IProperties;
import org.metaborg.meta.nabl2.solver.Properties;
import org.metaborg.meta.nabl2.solver.Solver;
import org.metaborg.meta.nabl2.solver.SolverComponent;
import org.metaborg.meta.nabl2.solver.UnsatisfiableException;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.util.Unit;

import com.google.common.collect.Sets;

public class AstSolver extends SolverComponent<IAstConstraint> {

    private final Properties<TermIndex> properties;

    public AstSolver(Solver solver) {
        super(solver);
        this.properties = new Properties<>();
    }

    public IProperties<TermIndex> getProperties() {
        return properties;
    }

    @Override protected Unit doAdd(IAstConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.<Unit, UnsatisfiableException>of(
            // @formatter:off
            p -> {
                solve(p);
                work();
                return unit;
            }
            // @formatter:on
        ));
    }

    private void solve(CAstProperty constraint) throws UnsatisfiableException {
        Optional<ITerm> oldValue =
                properties.putValue(constraint.getIndex(), constraint.getKey(), constraint.getValue());
        if(oldValue.isPresent()) {
            try {
                unifier().unify(oldValue.get(), constraint.getValue());
            } catch(UnificationException e) {
                throw new UnsatisfiableException(ImmutableMessageInfo.of(MessageKind.ERROR, e.getMessageContent(),
                        constraint.getMessageInfo().getOriginTerm()));
            }
        }
    }

    @Override protected Set<? extends IAstConstraint> doFinish(IMessageInfo messageInfo) throws InterruptedException {
        Set<IAstConstraint> constraints = Sets.newHashSet();
        if(isPartial()) {
            for(TermIndex index : properties.getIndices()) {
                for(ITerm key : properties.getDefinedKeys(index)) {
                    properties.getValue(index, key).ifPresent(value -> {
                        constraints.add(ImmutableCAstProperty.of(index, key, value, messageInfo));
                    });
                }
            }
        }
        return constraints;
    }

}