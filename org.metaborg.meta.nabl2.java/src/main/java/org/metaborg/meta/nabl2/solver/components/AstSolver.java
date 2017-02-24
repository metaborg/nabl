package org.metaborg.meta.nabl2.solver.components;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.List;
import java.util.Optional;

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

import com.google.common.collect.Lists;

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
        return constraint.matchOrThrow(CheckedCases.<Unit, UnsatisfiableException>of(p -> {
            Optional<ITerm> oldValue = properties.putValue(p.getIndex(), p.getKey(), p.getValue());
            if(oldValue.isPresent()) {
                try {
                    unifier().unify(oldValue.get(), p.getValue());
                } catch(UnificationException e) {
                    throw new UnsatisfiableException(ImmutableMessageInfo.of(MessageKind.ERROR, e.getMessageContent(),
                        constraint.getMessageInfo().getOriginTerm()));
                }
            }
            return unit;
        }));
    }

    @Override protected Iterable<? extends IAstConstraint> doFinish(IMessageInfo messageInfo)
        throws InterruptedException {
        List<IAstConstraint> constraints = Lists.newArrayList();
        if(isPartial()) {
            for(TermIndex index : properties.getIndices()) {
                for(ITerm key : properties.getDefinedKeys(index)) {
                    properties.getValue(index, key).map(unifier()::find).ifPresent(value -> {
                        constraints.add(ImmutableCAstProperty.of(index, key, value, messageInfo));
                    });
                }
            }
        }
        return constraints;
    }

}