package mb.nabl2.scopegraph.esop.bottomup;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.terms.SpacedName;

class BUEnv<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>> {

    @SuppressWarnings("unused") private static final ILogger logger = LoggerUtils.logger(BUEnv.class);

    private final BULabelOrder<L> compare;

    private BUPathSet.Immutable<S, L, O, P> paths;

    private BUPathSet.Transient<S, L, O, P> addedPaths;
    private BUPathSet.Transient<S, L, O, P> removedPaths;

    public BUEnv(BULabelOrder<L> compare) {
        this(compare, BUPathSet.Immutable.of());
    }

    BUEnv(BULabelOrder<L> compare, BUPathSet.Immutable<S, L, O, P> paths) {
        this.compare = compare;
        this.paths = paths;
        this.addedPaths = BUPathSet.Transient.of();
        this.removedPaths = BUPathSet.Transient.of();
    }

    public Collection<P> paths() {
        return paths.paths();
    }

    public Collection<P> paths(SpacedName name) {
        return paths.paths(name);
    }

    public void apply(BUChanges<S, L, O, P> changes) throws InterruptedException {
        final BUPathSet.Transient<S, L, O, P> paths = this.paths.melt();
        final BUPathSet.Immutable<S, L, O, P> addPaths = changes.addedPaths();
        for(SpacedName name : addPaths.names()) {
            for(BUPathKey<L> key : addPaths.keys(name)) {
                addPaths(name, key, paths, addPaths.paths(key));
            }
        }
        final BUPathSet.Immutable<S, L, O, P> removePaths = changes.removedPaths();
        for(SpacedName name : removePaths.names()) {
            for(BUPathKey<L> key : removePaths.keys(name)) {
                removePaths(name, key, paths, removePaths.paths(key));
            }
        }
        this.paths = paths.freeze();
    }

    private void removePaths(SpacedName name, BUPathKey<L> key, BUPathSet.Transient<S, L, O, P> paths,
            Collection<P> oldPaths) throws InterruptedException {
        oldPaths = paths.remove(key, oldPaths);
        addedPaths.remove(key, oldPaths);
        removedPaths.add(key, oldPaths);
    }

    private void addPaths(SpacedName name, BUPathKey<L> key, BUPathSet.Transient<S, L, O, P> paths,
            Collection<P> newPaths) throws InterruptedException {
        for(BUPathKey<L> otherKey : paths.keys(name)) {
            final Integer result = compare.test(key.label(), otherKey.label());
            if(result != null) {
                // labels are comparable
                if(result < 0) {
                    // the candidate is more specific than an earlier selected path
                    final Collection<P> otherPaths = paths.remove(otherKey);
                    addedPaths.remove(otherKey, otherPaths);
                    removedPaths.add(otherKey, otherPaths);
                }
                if(result > 0) {
                    // the candidate is less specific than an earlier selected path
                    return;
                }
            }
        }
        // there are no smaller pre-existing paths
        newPaths = paths.add(key, newPaths);
        addedPaths.add(key, newPaths);
    }

    public BUChanges<S, L, O, P> commit() throws InterruptedException {
        final BUChanges<S, L, O, P> changes = new BUChanges<>(addedPaths.freeze(), removedPaths.freeze());
        addedPaths = BUPathSet.Transient.of();
        removedPaths = BUPathSet.Transient.of();
        return changes;
    }

    public boolean hasChanges() {
        return !addedPaths.isEmpty() || !removedPaths.isEmpty();
    }

    public BUChanges<S, L, O, P> asChanges() {
        return new BUChanges<>(paths, BUPathSet.Immutable.of());
    }

    public void write(String prefix, Writer out) throws IOException {
        //        out.append(prefix + paths.keySet().size() + " names, " + paths.size() + " paths\n");
        /*
        for(SpacedName name : env.keySet()) {
            out.append(prefix + name + ": " + env.get(name).size() + "\n");
            for(P path : env.get(name)) {
                out.append(prefix + " - " + path + "\n");
            }
        }
        */
    }

}