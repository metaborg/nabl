package mb.nabl2.interpreter;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import mb.nabl2.constraints.namebinding.DeclProperties;
import mb.nabl2.scopegraph.IScopeGraph;
import mb.nabl2.scopegraph.esop.CriticalEdgeException;
import mb.nabl2.scopegraph.esop.IEsopNameResolution;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.collections.IProperties;

public class InterpreterTerms {

    private static final ILogger logger = LoggerUtils.logger(InterpreterTerms.class);

    public static ITerm context(ISolution solution) {
        return B.newAppl("NaBL2", scopegraph(solution.scopeGraph()),
                nameresolution(solution.scopeGraph().getAllRefs(), solution.nameResolution()),
                declTypes(solution.declProperties(), solution.unifier()));
    }

    private static ITerm scopegraph(IScopeGraph<Scope, Label, Occurrence> scopeGraph) {
        return B.newAppl("G", scopeEntries(scopeGraph), declEntries(scopeGraph), refEntries(scopeGraph));
    }

    private static ITerm scopeEntries(IScopeGraph<Scope, Label, Occurrence> scopeGraph) {
        Map<ITerm, ITerm> entries = Maps.newHashMap();
        for(Scope scope : scopeGraph.getAllScopes()) {
            IListTerm decls = B.newList(scopeGraph.getDecls().inverse().get(scope));
            IListTerm refs = B.newList(scopeGraph.getRefs().inverse().get(scope));
            IListTerm edges = multimap(scopeGraph.getDirectEdges().get(scope));
            IListTerm imports = multimap(scopeGraph.getImportEdges().get(scope));
            ITerm entry = B.newAppl("SE", decls, refs, edges, imports);
            entries.put(scope, entry);
        }
        return map(entries.entrySet());
    }

    private static ITerm declEntries(IScopeGraph<Scope, Label, Occurrence> scopeGraph) {
        Map<ITerm, ITerm> entries = Maps.newHashMap();
        for(Occurrence decl : scopeGraph.getAllDecls()) {
            ITerm scope = scopeGraph.getDecls().get(decl).map(s -> B.newList(s)).orElse(B.newNil());
            ITerm assocs = multimap(scopeGraph.getExportEdges().get(decl));
            ITerm entry = B.newAppl("DE", scope, assocs);
            entries.put(decl, entry);
        }
        return map(entries.entrySet());
    }

    private static ITerm refEntries(IScopeGraph<Scope, Label, Occurrence> scopeGraph) {
        Map<ITerm, ITerm> entries = Maps.newHashMap();
        for(Occurrence ref : scopeGraph.getAllRefs()) {
            ITerm scope = scopeGraph.getRefs().get(ref).map(s -> B.newList(s)).orElse(B.newNil());
            ITerm entry = B.newAppl("RE", scope);
            entries.put(ref, entry);
        }
        return map(entries.entrySet());
    }

    private static ITerm nameresolution(Iterable<Occurrence> refs,
            IEsopNameResolution<Scope, Label, Occurrence> nameResolution) {
        final Map<ITerm, ITerm> entries = Maps.newHashMap();
        for(Occurrence ref : refs) {
            try {
                Collection<IResolutionPath<Scope, Label, Occurrence>> paths = nameResolution.resolve(ref);
                if(paths.size() == 1) {
                    IResolutionPath<Scope, Label, Occurrence> path = Iterables.getOnlyElement(paths);
                    ITerm value = B.newTuple(path.getDeclaration(), Paths.toTerm(path));
                    entries.put(ref, value);
                } else {
                    logger.warn("Can only convert a single path, but {} has {}.", ref, paths.size());
                }
            } catch(CriticalEdgeException e) {
                logger.warn("Could not convert unresolvable {}.", ref);
            }
        }
        return map(entries.entrySet());
    }

    private static ITerm declTypes(IProperties<Occurrence, ITerm, ITerm> declProperties, IUnifier unifier) {
        Map<ITerm, ITerm> entries = Maps.newHashMap();
        for(Occurrence decl : declProperties.getIndices()) {
            declProperties.getValue(decl, DeclProperties.TYPE_KEY).map(unifier::findRecursive).ifPresent(type -> {
                entries.put(decl, unifier.findRecursive(type));
            });
        }
        return map(entries.entrySet());
    }

    // ---------------

    private static IListTerm map(Iterable<? extends Map.Entry<? extends ITerm, ? extends ITerm>> entries) {
        List<ITerm> entryTerms = Lists.newArrayList();
        for(Map.Entry<? extends ITerm, ? extends ITerm> entry : entries) {
            entryTerms.add(B.newTuple(entry.getKey(), entry.getValue()));
        }
        return B.newList(entryTerms);
    }

    private static IListTerm multimap(Iterable<? extends Map.Entry<? extends ITerm, ? extends ITerm>> entries) {
        Multimap<ITerm, ITerm> grouped = HashMultimap.create();
        for(Map.Entry<? extends ITerm, ? extends ITerm> entry : entries) {
            grouped.put(entry.getKey(), entry.getValue());
        }
        List<ITerm> entryterms = Lists.newArrayList();
        for(ITerm key : grouped.keySet()) {
            entryterms.add(B.newTuple(key, B.newList(grouped.get(key))));
        }
        return B.newList(entryterms);
    }

}