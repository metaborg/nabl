package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.spoofax.StatixTerms.explicateVars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.collection.Sets;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;


import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.IState;
import mb.statix.solver.persistent.SolverResult;

public class STX_debug_scopegraph extends StatixPrimitive {

    private static final ILogger logger = LoggerUtils.logger(STX_debug_scopegraph.class);

    @jakarta.inject.Inject @javax.inject.Inject public STX_debug_scopegraph() {
        super(STX_debug_scopegraph.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        // @formatter:off
        final List<SolverResult> analyses = M.cases(
            M.blobValue(SolverResult.class).map(ImList.Immutable::of),
            M.listElems(M.blobValue(SolverResult.class))
        ).match(term).orElseThrow(() -> new InterpreterException("Expected solver result."));
        // @formatter:on

        final Map<Scope, Map<ITerm, List<Scope>>> edgeEntries = new HashMap<>(); // Scope * (Label * Scope)
        final Map<Scope, Map<ITerm, List<ITerm>>> relationEntries = new HashMap<>(); // Scope * (Label * Scope)
        final Set<Scope> dataScopes = new HashSet<Scope>();
        for(SolverResult<?> analysis : analyses) {
            addScopeEntries(analysis, edgeEntries, relationEntries, dataScopes);
        }

        final List<ITerm> scopeEntries = new ArrayList<>(); // [InlinedEntry(Scope * [Label * [ITerm]] * [Label * [Scope]])]
        for(Scope scope : Sets.union(edgeEntries.keySet(), relationEntries.keySet())) {
            if(dataScopes.contains(scope)) {
                if((relationEntries.containsKey(scope) && !relationEntries.get(scope).isEmpty())) {
                    logger.warn("Data scope {} has relations {}. The inlined scope graph will be incomplete.", scope,
                            relationEntries.get(scope).keySet());
                }
                if((edgeEntries.containsKey(scope) && !edgeEntries.get(scope).isEmpty())) {
                    logger.warn("Data scope {} has edges {}. The inlined scope graph will be incomplete.", scope,
                            edgeEntries.get(scope));
                }
                continue;
            }
            final ITerm relations = Optional.ofNullable(relationEntries.get(scope)).map(rs -> {
                final List<ITerm> lblDatums = new ArrayList<>();
                rs.entrySet().forEach(rr -> {
                    final ITerm lbl_datum = B.newTuple(rr.getKey(), B.newList(explicateVars(rr.getValue())));
                    lblDatums.add(lbl_datum);
                });
                return B.newList(lblDatums);
            }).orElse(B.newList());
            final ITerm edges = Optional.ofNullable(edgeEntries.get(scope)).map(es -> {
                final List<ITerm> lblTgts = new ArrayList<>();
                es.entrySet().forEach(ee -> {
                    final ITerm lbl_tgt = B.newTuple(ee.getKey(), B.newList(explicateVars(ee.getValue())));
                    lblTgts.add(lbl_tgt);
                });
                return B.newList(lblTgts);
            }).orElse(B.newList());

            scopeEntries.add(B.newAppl("InlinedEntry", explicateVars(scope), relations, edges));
        }

        // @formatter:on
        return Optional.of(B.newAppl(StatixTerms.SCOPEGRAPH_OP, B.newList(scopeEntries)));
    }


    private void addScopeEntries(SolverResult<?> analysis, Map<Scope, Map<ITerm, List<Scope>>> edgeEntries,
            Map<Scope, Map<ITerm, List<ITerm>>> relationEntries, Set<Scope> dataScopes) {
        final IState.Immutable state = analysis.state();
        final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph = state.scopeGraph();
        final IUniDisunifier.Immutable unifier = state.unifier();

        final Set<ITerm> dataLabels = analysis.spec().dataLabels();

        scopeGraph.getEdges().forEach((src_lbl, tgts) -> {
            final Scope src = src_lbl.getKey();
            final ITerm lbl = src_lbl.getValue();
            if(dataLabels.contains(lbl)) {
                final Map<ITerm, List<ITerm>> relations = relationEntries.computeIfAbsent(src, k -> new HashMap<>());
                for(Scope tgt : tgts) {
                    dataScopes.add(tgt);
                    scopeGraph.getData(tgt).ifPresent(d -> {
                        d = unifier.findRecursive(d);
                        relations.computeIfAbsent(lbl, k -> new ArrayList<>()).add(d);
                    });
                }
            } else {
                final Map<ITerm, List<Scope>> edges = edgeEntries.computeIfAbsent(src, k -> new HashMap<>());
                edges.computeIfAbsent(lbl, k -> new ArrayList<>()).addAll(tgts);
            }
        });
    }

}
