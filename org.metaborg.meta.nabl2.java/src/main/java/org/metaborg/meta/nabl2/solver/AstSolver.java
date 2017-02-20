package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint;
import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.messages.ImmutableMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageKind;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.util.iterators.Iterables2;

public class AstSolver extends AbstractSolverComponent<IAstConstraint> {

    private final Unifier unifier;
    private final Properties<TermIndex> properties;

    public AstSolver(Unifier unifier) {
        this.unifier = unifier;
        this.properties = new Properties<>();
    }

    public IProperties<TermIndex> getProperties() {
        return properties;
    }

    @Override public Class<IAstConstraint> getConstraintClass() {
        return IAstConstraint.class;
    }
    
    @Override public Unit add(IAstConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.<Unit, UnsatisfiableException>of(p -> {
            Optional<ITerm> oldValue = properties.putValue(p.getIndex(), p.getKey(), p.getValue());
            if(oldValue.isPresent()) {
                try {
                    unifier.unify(oldValue.get(), p.getValue());
                } catch(UnificationException e) {
                    throw new UnsatisfiableException(ImmutableMessageInfo.of(MessageKind.ERROR, e.getMessageContent(),
                        constraint.getMessageInfo().getOriginTerm()));
                }
            }
            return unit;
        }));
    }

    @Override public boolean iterate() {
        return false;
    }

    @Override public Iterable<IAstConstraint> finish() {
        return Iterables2.empty();
    }

}