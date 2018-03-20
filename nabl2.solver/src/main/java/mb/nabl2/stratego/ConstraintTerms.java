package mb.nabl2.stratego;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.util.Ref;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.Lists;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

public class ConstraintTerms {

    private final static String LIST_CTOR = "CList";
    private final static String LISTTAIL_CTOR = "CListTail";
    private final static String VAR_CTOR = "CVar";

    private ConstraintTerms() {
    }

    /**
     * Specialize appl's of NaBL2 constructors for variables and lists to actual variables and lists.
     */
    public static ITerm specialize(ITerm term) {
        // fromStratego
        term = term.match(Terms.cases(
        // @formatter:off
            appl -> {
                List<ITerm> args = appl.getArgs().stream().map(arg -> specialize(arg)).collect(Collectors.toList());
                return B.newAppl(appl.getOp(), args);
            },
            list -> specializeList(list),
            string -> string,
            integer -> integer,
            blob -> blob,
            var -> { throw new IllegalArgumentException("Term is already specialized."); }
            // @formatter:on
        )).withAttachments(term.getAttachments());
        term = M.<ITerm>cases(
            // @formatter:off
            M.appl2(VAR_CTOR, M.stringValue(), M.stringValue(), (v, resource, name) ->
                    B.newVar(resource, name)),
            M.appl1(LIST_CTOR, M.list(), (t, xs) ->
                    xs),
            M.appl2(LISTTAIL_CTOR, M.listElems(), M.term(), (t, xs, ys) ->
                    B.newListTail(xs, (IListTerm) ys))
            // @formatter:on
        ).match(term).orElse(term);
        return term;
    }

    private static IListTerm specializeList(IListTerm list) {
        // fromStrategoList
        final List<ITerm> terms = Lists.newArrayList();
        final List<ImmutableClassToInstanceMap<Object>> attachments = Lists.newArrayList();
        final Ref<ITermVar> varTail = new Ref<>();
        while(list != null) {
            list = list.match(ListTerms.cases(
            // @formatter:off
                cons -> {
                    terms.add(specialize(cons.getHead()));
                    attachments.add(cons.getAttachments());
                    return cons.getTail();
                },
                nil -> {
                    attachments.add(nil.getAttachments());
                    return null;
                },
                var -> {
                    varTail.set(var);
                    return null;
                }
                // @formatter:on
            ));
        }
        if(varTail.get() != null) {
            return B.newListTail(terms, varTail.get(), attachments);
        } else {
            return B.newList(terms, attachments);
        }
    }

    public static <R> IMatcher<R> specialize(IMatcher<R> m) {
        return (term, unifier) -> m.match(specialize(term), unifier);
    }

    /**
     * Encode variables and lists in NaBL2 constructors.
     */
    public static ITerm explicate(ITerm term) {
        return explicate(term, false);
    }

    private static ITerm explicate(ITerm term, final boolean wasLocked) {
        term = term.match(Terms.cases(
        // @formatter:off
            appl -> {
                List<ITerm> args = appl.getArgs().stream().map(arg -> explicate(arg)).collect(Collectors.toList());
                return B.newAppl(appl.getOp(), args);
            },
            list -> explicateList(list),
            string -> string,
            integer -> integer,
            blob -> blob,
            var -> {
                List<ITerm> args = Arrays.asList(B.newString(var.getResource()), B.newString(var.getName()));
                return B.newAppl(VAR_CTOR, args);
            }
            // @formatter:on
        )).withAttachments(term.getAttachments());
        // FIXME: Quoting is not restored ATM, so two round-trips could cause
        // problems when AST contains special constructors.
        return term;
    }

    private static ITerm explicateList(IListTerm list) {
        // toStrategoList
        final List<ITerm> terms = Lists.newArrayList();
        final List<ImmutableClassToInstanceMap<Object>> attachments = Lists.newArrayList();
        final Ref<ITerm> varTail = new Ref<>();
        while(list != null) {
            list = list.match(ListTerms.cases(
            // @formatter:off
                cons -> {
                    terms.add(explicate(cons.getHead()));
                    attachments.add(cons.getAttachments());
                    return cons.getTail();
                },
                nil -> {
                    attachments.add(nil.getAttachments());
                    return null;
                },
                var -> {
                    varTail.set(explicate(var));
                    attachments.add(ImmutableClassToInstanceMap.builder().build());
                    return null;
                }
                // @formatter:on
            ));
        }
        list = B.newList(terms, attachments);
        if(varTail.get() != null) {
            return B.newAppl(LISTTAIL_CTOR, list, varTail.get());
        } else {
            return list;
        }
    }

    public static <R> IMatcher<R> explicate(IMatcher<R> m) {
        return (t, u) -> m.match(explicate(t), u);
    }

}