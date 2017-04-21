package org.metaborg.meta.nabl2.interpreter;

import static org.metaborg.meta.nabl2.terms.generic.TB.newAppl;
import static org.metaborg.meta.nabl2.terms.generic.TB.newList;
import static org.metaborg.meta.nabl2.terms.generic.TB.newNil;
import static org.metaborg.meta.nabl2.terms.generic.TB.newTuple;

import java.util.List;
import java.util.Map;

import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.solver.IProperties;
import org.metaborg.meta.nabl2.solver.ISolution;
import org.metaborg.meta.nabl2.spoofax.analysis.AnalysisTerms;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class InterpreterTerms {

    private static final ILogger logger = LoggerUtils.logger(InterpreterTerms.class);

    public static ITerm context(ISolution solution) {
        return newAppl("NaBL2", scopegraph(solution.getScopeGraph()), nameresolution(solution.getNameResolution()),
            declTypes(solution.getDeclProperties(), solution.getUnifier()));
    }

    private static ITerm scopegraph(IScopeGraph<Scope, Label, Occurrence> scopeGraph) {
        return newAppl("G", scopeEntries(scopeGraph), declEntries(scopeGraph), refEntries(scopeGraph));
    }

    private static ITerm scopeEntries(IScopeGraph<Scope, Label, Occurrence> scopeGraph) {
        Map<ITerm, ITerm> entries = Maps.newHashMap();
        for(Scope scope : scopeGraph.getAllScopes()) {
            IListTerm decls = newList(scopeGraph.getDecls().inverse().get(scope));
            IListTerm refs = newList(scopeGraph.getRefs().inverse().get(scope));
            IListTerm edges = multimap(scopeGraph.getDirectEdges().get(scope));
            IListTerm imports = multimap(scopeGraph.getImportEdges().get(scope));
            ITerm entry = newAppl("SE", decls, refs, edges, imports);
            entries.put(scope, entry);
        }
        return map(entries.entrySet());
    }

    private static ITerm declEntries(IScopeGraph<Scope, Label, Occurrence> scopeGraph) {
        Map<ITerm, ITerm> entries = Maps.newHashMap();
        for(Occurrence decl : scopeGraph.getAllDecls()) {
            ITerm scope = scopeGraph.getDecls().get(decl).map(s -> newList(s)).orElse(newNil());
            ITerm assocs = multimap(scopeGraph.getExportEdges().get(decl));
            ITerm entry = newAppl("DE", scope, assocs);
            entries.put(decl, entry);
        }
        return map(entries.entrySet());
    }

    private static ITerm refEntries(IScopeGraph<Scope, Label, Occurrence> scopeGraph) {
        Map<ITerm, ITerm> entries = Maps.newHashMap();
        for(Occurrence ref : scopeGraph.getAllRefs()) {
            ITerm scope = scopeGraph.getRefs().get(ref).map(s -> newList(s)).orElse(newNil());
            ITerm entry = newAppl("RE", scope);
            entries.put(ref, entry);
        }
        return map(entries.entrySet());
    }

    private static ITerm nameresolution(INameResolution<Scope, Label, Occurrence> nameResolution) {
        Map<ITerm, ITerm> entries = Maps.newHashMap();
        for(Occurrence ref : nameResolution.getAllRefs()) {
            List<IResolutionPath<Scope, Label, Occurrence>> paths = Lists.newArrayList(nameResolution.resolve(ref));
            if(paths.size() == 1) {
                IResolutionPath<Scope, Label, Occurrence> path = paths.get(0);
                ITerm value = TB.newTuple(path.getDeclaration(), Paths.toTerm(path));
                entries.put(ref, value);
            } else {
                logger.warn("Can only convert a single path, {} has multipe.", ref);
            }
        }
        return map(entries.entrySet());
    }

    private static ITerm declTypes(IProperties<Occurrence> declProperties, IUnifier unifier) {
        Map<ITerm, ITerm> entries = Maps.newHashMap();
        for(Occurrence decl : declProperties.getIndices()) {
            declProperties.getValue(decl, AnalysisTerms.TYPE_KEY).map(unifier::find).ifPresent(type -> {
                entries.put(decl, unifier.find(type));
            });
        }
        return map(entries.entrySet());
    }

    // ---------------

    private static IListTerm map(Iterable<? extends Map.Entry<? extends ITerm, ? extends ITerm>> entries) {
        List<ITerm> entryTerms = Lists.newArrayList();
        for(Map.Entry<? extends ITerm, ? extends ITerm> entry : entries) {
            entryTerms.add(newTuple(entry.getKey(), entry.getValue()));
        }
        return newList(entryTerms);
    }

    private static IListTerm multimap(Iterable<? extends Map.Entry<? extends ITerm, ? extends ITerm>> entries) {
        Multimap<ITerm, ITerm> grouped = HashMultimap.create();
        for(Map.Entry<? extends ITerm, ? extends ITerm> entry : entries) {
            grouped.put(entry.getKey(), entry.getValue());
        }
        List<ITerm> entryterms = Lists.newArrayList();
        for(ITerm key : grouped.keySet()) {
            entryterms.add(newTuple(key, newList(grouped.get(key))));
        }
        return newList(entryterms);
    }

}