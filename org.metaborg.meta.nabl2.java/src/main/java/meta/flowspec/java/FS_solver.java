package meta.flowspec.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;

import meta.flowspec.java.pcollections.MapSetPRelation;
import meta.flowspec.java.pcollections.PRelation;
import meta.flowspec.java.solver.MFP2;
import meta.flowspec.java.stratego.BuildSolverTerms;
import meta.flowspec.java.stratego.MatchSolverTerms;
import meta.flowspec.java.stratego.MatchTerm;
import meta.flowspec.java.stratego.TermMatchException;

public class FS_solver extends AbstractPrimitive {
    private static final ILogger logger = LoggerUtils.logger(FS_solver.class);

    public FS_solver() {
        super(FS_solver.class.getSimpleName(), 0, 1);
    }

    @Override
    public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        // PropName -> TermIndex -> ResultValue*
        final IStrategoTerm current = tvars[0];

        try {
            IStrategoTuple tuple = MatchTerm.tuple(current).orElseThrow(() -> new TermMatchException("tuple", current.toString()));
            if (tuple.getSubtermCount() != 5) {
                throw new TermMatchException("tuple of 5", tuple.toString());
            }
            List<IStrategoTerm> typedefs = MatchTerm.list(tuple.getSubterm(0)).orElseThrow(() -> new TermMatchException("list", tuple.getSubterm(0).toString()));
            Map<String, Type> types = new HashMap<>();
            for (IStrategoTerm td: typedefs) {
                final Pair<String, Type> pair = MatchSolverTerms.typeDef(td);
                types.put(pair.left(), pair.right());
            }
            List<IStrategoTerm> conds = MatchTerm.list(tuple.getSubterm(1)).orElseThrow(() -> new TermMatchException("list", tuple.getSubterm(1).toString()));
            List<Pair<Pair<String, TermIndex>, ConditionalRhs>> pairs = new ArrayList<>();
            for (IStrategoTerm cond : conds) {
                pairs.add(MatchSolverTerms.propConstraint(cond));
            }
            PRelation<Pair<String, TermIndex>, ConditionalRhs> conditional = new MapSetPRelation<>();
            for (Pair<Pair<String, TermIndex>, ConditionalRhs> pair : pairs) {
                conditional = conditional.plus(pair.left(), pair.right());
            }
            PRelation<Pair<String, TermIndex>, Pair<Rhs, Rhs>> results = MFP2.intraProcedural(conditional, types);
            env.setCurrent(new BuildSolverTerms(results).toIStrategoTerm(env.getFactory()));
        } catch (TermMatchException e) {
            logger.warn("Did not receive well-formed input: " + e.getMessage());
            return false;
        }
        return true;
    }

}
