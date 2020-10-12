package mb.nabl2.stratego;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Arrays;
import java.util.List;

import org.metaborg.util.Ref;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.build.Attachments;
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
    public static ITerm specialize(final ITerm term) {
        // fromStratego
        // @formatter:off
        ITerm newTerm = term.match(Terms.cases(
            appl -> {
                List<ITerm> args = appl.getArgs().stream().map(arg -> specialize(arg)).collect(ImmutableList.toImmutableList());
                return B.newAppl(appl.getOp(), args, term.getAttachments());
            },
            list -> specializeList(list),
            string -> string,
            integer -> integer,
            blob -> blob,
            var -> { throw new IllegalArgumentException("Term is already specialized."); }
        ));
        // @formatter:on
        // @formatter:off
        newTerm = M.preserveAttachments(M.<ITerm>cases(
            M.appl2(VAR_CTOR, M.stringValue(), M.stringValue(), (v, resource, name) ->
                    B.newVar(resource, name)),
            M.appl1(LIST_CTOR, M.list(), (t, xs) ->
                    xs),
            M.appl2(LISTTAIL_CTOR, M.listElems(), M.term(), (t, xs, ys) ->
                    B.newListTail(xs, (IListTerm) ys))
        )).match(newTerm).orElse(newTerm);
        // @formatter:on
        return term;
    }

    private static IListTerm specializeList(IListTerm list) {
        // fromStrategoList
        final List<ITerm> terms = Lists.newArrayList();
        final List<IAttachments> attachments = Lists.newArrayList();
        final Ref<ITermVar> varTail = new Ref<>();
        while(list != null) {
            // @formatter:off
            list = list.match(ListTerms.cases(
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
            ));
            // @formatter:on
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
        // @formatter:off
        return term.match(Terms.cases(
            appl -> {
                List<ITerm> args = appl.getArgs().stream().map(arg -> explicate(arg)).collect(ImmutableList.toImmutableList());
                return B.newAppl(appl.getOp(), args, term.getAttachments());
            },
            list -> explicate(list),
            string -> string,
            integer -> integer,
            blob -> blob,
            var -> explicate(var)
        ));
        // @formatter:on
    }

    private static ITerm explicate(IListTerm list) {
        // toStrategoList
        final List<ITerm> terms = Lists.newArrayList();
        final List<IAttachments> attachments = Lists.newArrayList();
        final Ref<ITerm> varTail = new Ref<>();
        while(list != null) {
            // @formatter:off
            list = list.match(ListTerms.cases(
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
                    attachments.add(Attachments.empty()); // necessary?
                    return null;
                }
            ));
            // @formatter:on
        }
        list = B.newList(terms, attachments);
        if(varTail.get() != null) {
            return B.newAppl(LISTTAIL_CTOR, ImmutableList.of(list, varTail.get()));
        } else {
            return list;
        }
    }

    private static ITerm explicate(ITermVar var) {
        return B.newAppl(VAR_CTOR, Arrays.asList(B.newString(var.getResource()), B.newString(var.getName())));
    }

    public static <R> IMatcher<R> explicate(IMatcher<R> m) {
        return (t, u) -> m.match(explicate(t), u);
    }

}