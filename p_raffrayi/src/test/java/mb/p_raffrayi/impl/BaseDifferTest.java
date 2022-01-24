package mb.p_raffrayi.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.future.IFuture;

import com.google.common.collect.ImmutableList;

import mb.p_raffrayi.impl.diff.IDifferDataOps;
import mb.p_raffrayi.impl.diff.IDifferOps;
import mb.scopegraph.oopsla20.diff.BiMap;

public abstract class BaseDifferTest {

    protected static class TestDifferOps implements IDifferOps<String, Integer, List<String>> {

        public static final TestDifferOps instance = new TestDifferOps();

        private TestDifferOps() {
        }

        @Override public boolean isMatchAllowed(String currentScope, String previousScope) {
            return true;
        }

        @Override public Optional<BiMap.Immutable<String>> matchDatums(List<String> currentDatum,
                List<String> previousDatum) {
            if(currentDatum.size() == previousDatum.size()) {
                final BiMap.Transient<String> matches = BiMap.Transient.of();
                final Iterator<String> pIterator = previousDatum.iterator();
                for(String scope : currentDatum) {
                    matches.put(scope, pIterator.next());
                }
                return Optional.of(matches.freeze());
            }
            return Optional.empty();
        }

        @Override public Collection<String> getScopes(List<String> d) {
            return d;
        }

        @Override public List<String> embed(String scope) {
            return ImmutableList.of(scope);
        }

        @Override public boolean ownScope(String scope) {
            return true;
        }

        @Override public boolean ownOrSharedScope(String currentScope) {
            return true;
        }

        @Override public IFuture<Optional<String>> externalMatch(String previousScope) {
            throw new UnsupportedOperationException();
        }

    }

    protected static class TestDifferDataOps implements IDifferDataOps<List<String>> {

        public static final TestDifferDataOps instance = new TestDifferDataOps();

        private TestDifferDataOps() {
        }

        @Override public List<String> getExternalRepresentation(List<String> datum) {
            return datum;
        }

    }

}
