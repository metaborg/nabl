package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.spoofax.StatixTerms.explicateVars;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import javax.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.IState;
import mb.statix.solver.persistent.SolverResult;

public class STX_debug_scopegraph extends StatixPrimitive {

    private static final ILogger logger = LoggerUtils.logger(STX_debug_scopegraph.class);

    @Inject public STX_debug_scopegraph() {
        super(STX_debug_scopegraph.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        // @formatter:off
        final List<SolverResult> analyses = M.cases(
            M.blobValue(SolverResult.class).map(ImmutableList::of),
            M.listElems(M.blobValue(SolverResult.class))
        ).match(term).orElseThrow(() -> new InterpreterException("Expected solver result."));
        // @formatter:on

        final Map<Scope, ListMultimap<ITerm, Scope>> edgeEntries = Maps.newHashMap(); // Scope * (Label * Scope)
        final Map<Scope, ListMultimap<ITerm, ITerm>> relationEntries = Maps.newHashMap(); // Scope * (Label * Scope)
        final Set<Scope> dataScopes = Sets.newHashSet();
        for(SolverResult analysis : analyses) {
            addScopeEntries(analysis, edgeEntries, relationEntries, dataScopes);
        }

        final List<ITerm> scopeEntries = Lists.newArrayList(); // [InlinedEntry(Scope * [Label * [ITerm]] * [Label * [Scope]])]
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
                final List<ITerm> lblDatums = Lists.newArrayList();
                rs.asMap().entrySet().forEach(rr -> {
                    final ITerm lbl_datum = B.newTuple(rr.getKey(), B.newList(explicateVars(rr.getValue())));
                    lblDatums.add(lbl_datum);
                });
                return B.newList(lblDatums);
            }).orElse(B.newList());
            final ITerm edges = Optional.ofNullable(edgeEntries.get(scope)).map(es -> {
                final List<ITerm> lblTgts = Lists.newArrayList();
                es.asMap().entrySet().forEach(ee -> {
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

    private void addScopeEntries(SolverResult analysis, Map<Scope, ListMultimap<ITerm, Scope>> edgeEntries,
            Map<Scope, ListMultimap<ITerm, ITerm>> relationEntries, Set<Scope> dataScopes) {
        final IState.Immutable state = analysis.state();
        final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph = state.scopeGraph();
        final IUniDisunifier.Immutable unifier = state.unifier();

        final Set<ITerm> dataLabels = analysis.spec().dataLabels();

        scopeGraph.getEdges().forEach((src_lbl, tgts) -> {
            final Scope src = src_lbl.getKey();
            final ITerm lbl = src_lbl.getValue();
            if(dataLabels.contains(lbl)) {
                ListMultimap<ITerm, ITerm> relations = relationEntries.getOrDefault(src, LinkedListMultimap.create());
                for(Scope tgt : tgts) {
                    dataScopes.add(tgt);
                    scopeGraph.getData(tgt).ifPresent(d -> {
                        d = unifier.findRecursive(d);
                        relations.put(lbl, d);
                    });
                }
                relationEntries.put(src, relations);
            } else {
                ListMultimap<ITerm, Scope> edges = edgeEntries.getOrDefault(src, LinkedListMultimap.create());
                edges.putAll(lbl, tgts);
                edgeEntries.put(src, edges);
            }
        });
    }

}