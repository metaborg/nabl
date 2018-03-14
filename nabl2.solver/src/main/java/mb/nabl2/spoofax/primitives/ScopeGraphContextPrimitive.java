package mb.nabl2.spoofax.primitives;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.concurrent.NullClosableLock;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import mb.nabl2.config.NaBL2Config;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;
import mb.nabl2.spoofax.analysis.CustomSolution;
import mb.nabl2.spoofax.analysis.IScopeGraphContext;
import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.stratego.StrategoTerms;
import mb.nabl2.terms.ITerm;

public abstract class ScopeGraphContextPrimitive extends AbstractPrimitive {

    private static ILogger logger = LoggerUtils.logger(ScopeGraphContextPrimitive.class);

    public ScopeGraphContextPrimitive(String name, int svars, int tvars) {
        super(name, svars, tvars);
    }

    @Override public final boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars)
            throws InterpreterException {
        final Object contextObj = env.contextObject();
        if(contextObj == null) {
            logger.warn("Context is null.");
            return false;
        }
        final IScopeGraphContext<?> context;
        if(!(contextObj instanceof IScopeGraphContext)) {
            context = new FakeScopeGraphContext();
        } else {
            context = (IScopeGraphContext<?>) env.contextObject();
        }
        List<IStrategoTerm> termArgs = Arrays.asList(tvars);
        Optional<? extends IStrategoTerm> result;
        try(IClosableLock lock = context.guard()) {
            result = call(context, env.current(), termArgs, env.getFactory());
        }
        return result.map(t -> {
            env.setCurrent(t);
            return true;
        }).orElse(false);
    }

    public Optional<? extends IStrategoTerm> call(IScopeGraphContext<?> context, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        StrategoTerms strategoTerms = new StrategoTerms(factory);
        ITerm term = ConstraintTerms.specialize(strategoTerms.fromStratego(sterm));
        List<ITerm> terms = sterms.stream().map(strategoTerms::fromStratego).map(ConstraintTerms::specialize)
                .collect(Collectors.toList());
        Optional<? extends ITerm> result = call(context, term, terms);
        return result.map(ConstraintTerms::explicate).map(strategoTerms::toStratego);
    }

    @SuppressWarnings("unused") public Optional<? extends ITerm> call(IScopeGraphContext<?> context, ITerm term,
            List<ITerm> terms) throws InterpreterException {
        throw new UnsupportedOperationException("Subclasses must override call method.");
    }

    private static final class FakeScopeGraphContext implements IScopeGraphContext<IScopeGraphUnit> {
        IScopeGraphUnit fakeUnit = new IScopeGraphUnit() {
            private static final long serialVersionUID = 1L;
            private Fresh fresh = new Fresh();

            @Override
            public String resource() {
                return null;
            }

            @Override
            public Set<IConstraint> constraints() {
                return Collections.unmodifiableSet(new HashSet<>());
            }

            @Override
            public Optional<ISolution> solution() {
                return Optional.empty();
            }

            @Override
            public Optional<CustomSolution> customSolution() {
                return Optional.empty();
            }

            @Override
            public Fresh fresh() {
                return fresh;
            }

            @Override
            public boolean isPrimary() {
                return false;
            }
        };
        Collection<IScopeGraphUnit> fakeUnits = Collections.unmodifiableList(Arrays.asList(fakeUnit));
        IClosableLock l = new NullClosableLock();

        @Override
        public IClosableLock guard() {
            return l;
        }

        @Override
        public IScopeGraphUnit unit(String resource) {
            return fakeUnit;
        }

        @Override
        public Collection<IScopeGraphUnit> units() {
            return fakeUnits;
        }

        @Override
        public NaBL2Config config() {
            return NaBL2Config.DEFAULT;
        }
    }

}