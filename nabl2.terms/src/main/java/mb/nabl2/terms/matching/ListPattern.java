package mb.nabl2.terms.matching;

import static mb.nabl2.terms.matching.TermPattern.P;

import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;

public class ListPattern {

    public static LP LP = new LP();

    public static class LP {

        public Pattern newList(Iterable<? extends Pattern> args) {
            return newListTail(args, newNil());
        }

        public Pattern newListTail(Iterable<? extends Pattern> args, Pattern tail) {
            Pattern list = tail;
            for(Pattern elem : ImmutableList.copyOf(args).reverse()) {
                list = newCons(elem, list);
            }
            return list;
        }

        public Pattern newCons(Pattern head, /*List*/Pattern tail) {
            return P.newAppl(ListTerms.CONS_OP, head, tail);
        }

        public Pattern newNil() {
            return P.newAppl(ListTerms.NIL_OP);
        }

        public Pattern fromList(IListTerm term) {
            return fromList(term, v -> false);
        }

        public Pattern fromList(IListTerm list, Predicate1<ITermVar> isWildcard) {
            // @formatter:off
            return list.match(ListTerms.<Pattern>cases(
                cons -> newCons(P.fromTerm(cons.getHead()), fromList(cons.getTail())),
                nil -> newNil(),
                var -> isWildcard.test(var) ? new PatternVar() : new PatternVar(var)
            ));
            // @formatter:on
        }


    }

}