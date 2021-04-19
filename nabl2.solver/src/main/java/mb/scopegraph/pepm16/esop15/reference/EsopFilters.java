package mb.scopegraph.pepm16.esop15.reference;

import java.io.Serializable;
import java.util.Optional;

import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.path.IDeclPath;
import mb.scopegraph.pepm16.path.IPath;
import mb.scopegraph.pepm16.path.IResolutionPath;
import mb.scopegraph.pepm16.terms.path.Paths;

public class EsopFilters {

    interface Filter<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            extends Serializable {

        Optional<P> test(IDeclPath<S, L, O> path);

        Object matchToken(P p);

        boolean shortCircuit();

    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Filter<S, L, O, IResolutionPath<S, L, O>>
            resolutionFilter(O ref) {
        return new Filter<S, L, O, IResolutionPath<S, L, O>>() {
            private static final long serialVersionUID = 42L;

            @Override public Optional<IResolutionPath<S, L, O>> test(IDeclPath<S, L, O> path) {
                return Paths.resolve(ref, path);
            }

            @Override public Object matchToken(IResolutionPath<S, L, O> p) {
                return p.getDeclaration().getSpacedName();
            }

            @Override public boolean shortCircuit() {
                return true;
            }

        };
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Filter<S, L, O, IDeclPath<S, L, O>>
            envFilter() {
        return new Filter<S, L, O, IDeclPath<S, L, O>>() {
            private static final long serialVersionUID = 42L;

            @Override public Optional<IDeclPath<S, L, O>> test(IDeclPath<S, L, O> path) {
                return Optional.of(path);
            }

            @Override public Object matchToken(IDeclPath<S, L, O> p) {
                return p.getDeclaration().getSpacedName();
            }

            @Override public boolean shortCircuit() {
                return false;
            }

        };
    }

}