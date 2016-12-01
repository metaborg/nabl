package org.metaborg.meta.nabl2.spoofax;

import java.util.Optional;

import org.metaborg.meta.nabl2.ScopeGraphException;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.stratego.StrategoConstraints;
import org.metaborg.meta.nabl2.stratego.StrategoMatchers;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class Results {

    private final StrategoConstraints constraintBuilder;

    public Results(StrategoConstraints constraintBuilder) {
        this.constraintBuilder = constraintBuilder;
    }

    public InitialResult initialOf(IStrategoTerm term) throws ScopeGraphException {
        return StrategoMatchers.<InitialResult, ScopeGraphException> patternsThrows()
            // @formatter:off
            .appl2("InitialResult", (constraintTerm, argsTerm) -> {
                Iterable<IConstraint> constraints = constraintBuilder.fromStratego(constraintTerm);
                return ImmutableInitialResult.of(constraints, paramsOf(argsTerm), typeOf(argsTerm));
            })
            .otherwise(() -> { throw new ScopeGraphException("Cannot match "+term); })
            .match(term);
            // @formatter:on
    }

    public UnitResult unitOf(IStrategoTerm term) throws ScopeGraphException {
        return StrategoMatchers.<UnitResult, ScopeGraphException> patternsThrows()
            // @formatter:off
            .appl2("UnitResult", (ast, constraintTerm) -> {
                Iterable<IConstraint> constraints = constraintBuilder.fromStratego(constraintTerm);
                return ImmutableUnitResult.of(ast, constraints);
            })
            .otherwise(() -> { throw new ScopeGraphException("Cannot match "+term); })
            .match(term);
            // @formatter:on
    }

    public FinalResult finalOf(IStrategoTerm term) throws ScopeGraphException {
        return StrategoMatchers.<FinalResult, ScopeGraphException> patternsThrows()
            // @formatter:off
            .appl0("FinalResult", () -> {
                return ImmutableFinalResult.of();
            })
            .otherwise(() -> { throw new ScopeGraphException(); })
            .match(term);
            // @formatter:on
    }

    private IStrategoTerm paramsOf(IStrategoTerm term) throws ScopeGraphException {
        return StrategoMatchers.<IStrategoTerm, ScopeGraphException> patternsThrows()
            // @formatter:off
            .appl1("Params", ps -> ps)
            .appl2("ParamsAndType", (ps, t) -> ps)
            .otherwise(() -> { throw new ScopeGraphException("Cannot match "+term); })
            .match(term);
            // @formatter:on
    }

    private Optional<IStrategoTerm> typeOf(IStrategoTerm term) throws ScopeGraphException {
        return StrategoMatchers.<Optional<IStrategoTerm>, ScopeGraphException> patternsThrows()
            // @formatter:off
            .appl1("Params", ps -> Optional.empty())
            .appl2("ParamsAndType", (ps, t) -> Optional.of(t))
            .otherwise(() -> { throw new ScopeGraphException("Cannot match "+term); })
            .match(term);
            // @formatter:on
    }

}