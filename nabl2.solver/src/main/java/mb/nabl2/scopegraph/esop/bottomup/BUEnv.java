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

    private BUPathSet.Immutable<S, L, O, P> currentPaths;
    private final BUPathSet.Transient<S, L, O, P> deprecatedPaths;

    private BUPathSet.Transient<S, L, O, P> addedPaths;
    private BUPathSet.Transient<S, L, O, P> removedPaths;

    public BUEnv(BULabelOrder<L> compare) {
        this(compare, BUPathSet.Immutable.of());
        this.addedPaths = BUPathSet.Transient.of();
        this.removedPaths = BUPathSet.Transient.of();
    }

    BUEnv(BULabelOrder<L> compare, BUPathSet.Immutable<S, L, O, P> paths) {
        this.compare = compare;
        this.currentPaths = paths;
        this.deprecatedPaths = BUPathSet.Transient.of();
    }

    public BUPathSet.Immutable<S, L, O, P> pathSet() {
        return currentPaths;
    }

    public void apply(BUChanges<S, L, O, P> changes) throws InterruptedException {
        changes = changes.filter((key, p) -> !deprecatedPaths.paths(key).contains(p));
        final BUPathSet.Transient<S, L, O, P> paths = this.currentPaths.melt();
        final BUPathSet.Immutable<S, L, O, P> addPaths = changes.addedPaths();
        for(SpacedName name : addPaths.names()) {
            for(BUPathKey<L> key : addPaths.keys(name)) {
                addPaths(name, key, addPaths.paths(key), paths);
            }
        }
        final BUPathSet.Immutable<S, L, O, P> removePaths = changes.removedPaths();
        for(SpacedName name : removePaths.names()) {
            for(BUPathKey<L> key : removePaths.keys(name)) {
                removePaths(name, key, removePaths.paths(key), paths);
            }
        }
        this.currentPaths = paths.freeze();
    }

    private void removePaths(SpacedName name, BUPathKey<L> key, Collection<P> oldPaths,
            BUPathSet.Transient<S, L, O, P> paths) throws InterruptedException {
        oldPaths = paths.remove(key, oldPaths);
        addedPaths.remove(key, oldPaths);
        removedPaths.add(key, oldPaths);
        deprecatedPaths.add(key, oldPaths);
    }

    private void addPaths(SpacedName name, BUPathKey<L> key, Collection<P> newPaths,
            BUPathSet.Transient<S, L, O, P> paths) throws InterruptedException {
        for(BUPathKey<L> otherKey : paths.keys(name)) {
            final Integer result = compare.test(key.label(), otherKey.label());
            if(result != null) {
                // labels are comparable
                if(result < 0) {
                    // the candidate is more specific than an earlier selected path
                    final Collection<P> otherPaths = paths.remove(otherKey);
                    addedPaths.remove(otherKey, otherPaths);
                    removedPaths.add(otherKey, otherPaths);
                    deprecatedPaths.add(key, otherPaths);
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

    public BUChanges<S, L, O, P> commitChanges() {
        BUChanges<S, L, O, P> changes = new BUChanges<>(addedPaths.freeze(), removedPaths.freeze());
        this.addedPaths = BUPathSet.Transient.of();
        this.removedPaths = BUPathSet.Transient.of();
        return changes;
    }

    public boolean hasChanges() {
        return !addedPaths.isEmpty() || !removedPaths.isEmpty();
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