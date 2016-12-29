package org.metaborg.meta.nabl2.interpreter;

import static org.metaborg.meta.nabl2.terms.generic.GenericTerms.newAppl;
import static org.metaborg.meta.nabl2.terms.generic.GenericTerms.newList;
import static org.metaborg.meta.nabl2.terms.generic.GenericTerms.newNil;
import static org.metaborg.meta.nabl2.terms.generic.GenericTerms.newTuple;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IPath;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Paths;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.IProperties;
import org.metaborg.meta.nabl2.solver.ISolution;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.util.functions.PartialFunction0;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class InterpreterTerms {

    private static final ILogger logger = LoggerUtils.logger(InterpreterTerms.class);

    public static ITerm context(ISolution solution) {
        return newAppl("NaBL2", scopegraph(solution.getScopeGraph()), nameresolution(solution.getNameResolution()),
                declTypes(solution.getDeclProperties(), solution.getUnifier()));
    }

    private static ITerm scopegraph(IScopeGraph<Scope,Label,Occurrence> scopeGraph) {
        return newAppl("G", scopeEntries(scopeGraph), declEntries(scopeGraph), refEntries(scopeGraph));
    }

    private static ITerm scopeEntries(IScopeGraph<Scope,Label,Occurrence> scopeGraph) {
        Map<ITerm,ITerm> entries = Maps.newHashMap();
        for (Scope scope : scopeGraph.getAllScopes()) {
            IListTerm decls = newList(scopeGraph.getDecls(scope));
            IListTerm refs = newList(scopeGraph.getRefs(scope));
            IListTerm edges = partialMultimap(scopeGraph.getDirectEdges(scope));
            IListTerm imports = partialMultimap(scopeGraph.getImportRefs(scope));
            ITerm entry = newAppl("SE", decls, refs, edges, imports);
            entries.put(scope, entry);
        }
        return map(entries);
    }

    private static ITerm declEntries(IScopeGraph<Scope,Label,Occurrence> scopeGraph) {
        Map<ITerm,ITerm> entries = Maps.newHashMap();
        for (Occurrence decl : scopeGraph.getAllDecls()) {
            ITerm scope = scopeGraph.getDeclScope(decl).map(s -> newList(s)).orElse(newNil());
            ITerm assocs = multimap(scopeGraph.getAssocScopes(decl));
            ITerm entry = newAppl("DE", scope, assocs);
            entries.put(decl, entry);
        }
        return map(entries);
    }

    private static ITerm refEntries(IScopeGraph<Scope,Label,Occurrence> scopeGraph) {
        Map<ITerm,ITerm> entries = Maps.newHashMap();
        for (Occurrence ref : scopeGraph.getAllRefs()) {
            ITerm scope = scopeGraph.getRefScope(ref).map(s -> newList(s)).orElse(newNil());
            ITerm entry = newAppl("RE", scope);
            entries.put(ref, entry);
        }
        return map(entries);
    }

    private static ITerm nameresolution(INameResolution<Scope,Label,Occurrence> nameResolution) {
        Map<ITerm,ITerm> entries = Maps.newHashMap();
        for (Occurrence ref : nameResolution.getAllRefs()) {
            List<IPath<Scope,Label,Occurrence>> paths = Lists.newArrayList(nameResolution.resolve(ref));
            if (paths.size() == 1) {
                IPath<Scope,Label,Occurrence> path = paths.get(0);
                ITerm value = GenericTerms.newTuple(path.getDeclaration(), Paths.toTerm(path));
                entries.put(ref, value);
            } else {
                logger.warn("Can only convert a single path, {} has multipe.", ref);
            }
        }
        return map(entries);
    }

    private static ITerm declTypes(IProperties<Occurrence> declProperties, IUnifier unifier) {
        ITerm key = newAppl("Type");
        Map<ITerm,ITerm> entries = Maps.newHashMap();
        for (Occurrence decl : declProperties.getIndices()) {
            declProperties.getValue(decl, key).map(unifier::find).ifPresent(type -> {
                entries.put(decl, unifier.find(type));
            });
        }
        return map(entries);
    }

    // ---------------

    private static IListTerm map(Map<ITerm,ITerm> map) {
        List<ITerm> entries = Lists.newArrayList();
        for (Map.Entry<? extends ITerm,? extends ITerm> entry : map.entrySet()) {
            entries.add(newTuple(entry.getKey(), entry.getValue()));
        }
        return newList(entries);
    }

    private static <K extends ITerm, V extends ITerm> IListTerm multimap(Multimap<K,V> map) {
        List<ITerm> entries = Lists.newArrayList();
        for (Map.Entry<K,Collection<V>> entry : map.asMap().entrySet()) {
            entries.add(newTuple(entry.getKey(), newList(entry.getValue())));
        }
        return newList(entries);
    }

    private static <K extends ITerm, V extends ITerm> IListTerm partialMultimap(Multimap<K,PartialFunction0<V>> map) {
        List<ITerm> entries = Lists.newArrayList();
        for (Map.Entry<K,Collection<PartialFunction0<V>>> entry : map.asMap().entrySet()) {
            List<ITerm> values = entry.getValue().stream().map(PartialFunction0::apply).filter(Optional::isPresent).map(
                    Optional::get).collect(Collectors.toList());
            entries.add(newTuple(entry.getKey(), newList(values)));
        }
        return newList(entries);
    }

}