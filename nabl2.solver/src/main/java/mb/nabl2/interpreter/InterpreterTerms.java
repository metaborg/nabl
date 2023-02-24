package mb.nabl2.interpreter;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;

import io.usethesource.capsule.SetMultimap;
import mb.nabl2.constraints.namebinding.DeclProperties;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.collections.IProperties;
import mb.scopegraph.pepm16.CriticalEdgeException;
import mb.scopegraph.pepm16.IScopeGraph;
import mb.scopegraph.pepm16.StuckException;
import mb.scopegraph.pepm16.esop15.IEsopNameResolution;
import mb.scopegraph.pepm16.path.IResolutionPath;
import mb.scopegraph.pepm16.terms.Label;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.Scope;
import mb.scopegraph.pepm16.terms.path.Paths;

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
        Map<ITerm, ITerm> entries = new HashMap<>();
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
        Map<ITerm, ITerm> entries = new HashMap<>();
        for(Occurrence decl : scopeGraph.getAllDecls()) {
            ITerm scope = scopeGraph.getDecls().get(decl).map(s -> B.newList(s)).orElse(B.newNil());
            ITerm assocs = multimap(scopeGraph.getExportEdges().get(decl));
            ITerm entry = B.newAppl("DE", scope, assocs);
            entries.put(decl, entry);
        }
        return map(entries.entrySet());
    }

    private static ITerm refEntries(IScopeGraph<Scope, Label, Occurrence> scopeGraph) {
        Map<ITerm, ITerm> entries = new HashMap<>();
        for(Occurrence ref : scopeGraph.getAllRefs()) {
            ITerm scope = scopeGraph.getRefs().get(ref).map(s -> B.newList(s)).orElse(B.newNil());
            ITerm entry = B.newAppl("RE", scope);
            entries.put(ref, entry);
        }
        return map(entries.entrySet());
    }

    private static ITerm nameresolution(Iterable<Occurrence> refs,
            IEsopNameResolution<Scope, Label, Occurrence> nameResolution) {
        final Map<ITerm, ITerm> entries = new HashMap<>();
        try {
            for(Occurrence ref : refs) {
                try {
                    Collection<IResolutionPath<Scope, Label, Occurrence>> paths = nameResolution.resolve(ref, new NullCancel(), new NullProgress());
                    if(paths.size() == 1) {
                        IResolutionPath<Scope, Label, Occurrence> path = paths.iterator().next();
                        ITerm value = B.newTuple(path.getDeclaration(), Paths.toTerm(path));
                        entries.put(ref, value);
                    } else {
                        logger.warn("Can only convert a single path, but {} has {}.", ref, paths.size());
                    }
                } catch(CriticalEdgeException | StuckException e) {
                    logger.warn("Could not convert unresolvable {}.", ref);
                }
            }
        } catch(InterruptedException e) {
            logger.warn("Conversion interrupted.");
        }
        return map(entries.entrySet());
    }

    private static ITerm declTypes(IProperties<Occurrence, ITerm, ITerm> declProperties, IUnifier unifier) {
        Map<ITerm, ITerm> entries = new HashMap<>();
        for(Occurrence decl : declProperties.getIndices()) {
            declProperties.getValue(decl, DeclProperties.TYPE_KEY).map(unifier::findRecursive).ifPresent(type -> {
                entries.put(decl, unifier.findRecursive(type));
            });
        }
        return map(entries.entrySet());
    }

    // ---------------

    private static IListTerm map(Iterable<? extends Map.Entry<? extends ITerm, ? extends ITerm>> entries) {
        List<ITerm> entryTerms = new ArrayList<>();
        for(Map.Entry<? extends ITerm, ? extends ITerm> entry : entries) {
            entryTerms.add(B.newTuple(entry.getKey(), entry.getValue()));
        }
        return B.newList(entryTerms);
    }

    private static IListTerm multimap(Iterable<? extends Map.Entry<? extends ITerm, ? extends ITerm>> entries) {
        SetMultimap.Transient<ITerm, ITerm> grouped = SetMultimap.Transient.of();
        for(Map.Entry<? extends ITerm, ? extends ITerm> entry : entries) {
            grouped.__put(entry.getKey(), entry.getValue());
        }
        List<ITerm> entryterms = new ArrayList<>();
        for(ITerm key : grouped.keySet()) {
            entryterms.add(B.newTuple(key, B.newList(grouped.get(key))));
        }
        return B.newList(entryterms);
    }

}