package mb.statix.taico.util;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.scopegraph.terms.Scope;

public class ScopesTest {

    /**
     * Tests that all the scopes in a complex term are retrieved by the
     * {@link Scopes#getScopesInTerm(ITerm, IUnifier)} method.
     */
    @Test
    public void testGetScopesInTerm() throws OccursException {
        //Build a very complex term consisting of tuples, lists, appls, scopes, variables, strings, ints and blobs
        ITerm term = B.newTuple(
                Scope.of("", "A"),
                B.newList(Scope.of("", "B"), B.newVar("", "v")),
                B.newAppl("Class", Scope.of("", "C"), B.newList(Scope.of("", "D"))),
                B.newTuple(Scope.of("", "E"), B.newString("x"), B.newInt(1)),
                B.newTuple(),
                B.newBlob(new Object())
        );
        
        //Use one variable in the term to check that one
        IUnifier.Transient unifier = PersistentUnifier.Transient.of();
        IUnifier.Immutable unifier2 = unifier.unify(B.newVar("", "v"), Scope.of("", "F")).get();
        
        //Verify that all the added scopes are found
        Set<Scope> scopes = Scopes.getScopesInTerm(term, unifier2);
        assertTrue(scopes.contains(Scope.of("", "A")));
        assertTrue(scopes.contains(Scope.of("", "B")));
        assertTrue(scopes.contains(Scope.of("", "C")));
        assertTrue(scopes.contains(Scope.of("", "D")));
        assertTrue(scopes.contains(Scope.of("", "E")));
        assertTrue(scopes.contains(Scope.of("", "F")));
    }

}
