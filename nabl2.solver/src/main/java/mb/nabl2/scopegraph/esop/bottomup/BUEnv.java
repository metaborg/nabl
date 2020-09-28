package mb.nabl2.scopegraph.esop.bottomup;

import java.io.IOException;
import java.io.Writer;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.terms.SpacedName;
import mb.nabl2.util.CapsuleUtil;

public class BUEnv<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>> {

    @SuppressWarnings("unused") private static final ILogger logger = LoggerUtils.logger(BUEnv.class);

    private final Function2<P, P, Integer> compare;
    private final SetMultimap.Transient<SpacedName, P> paths;

    private Set.Transient<P> addedPaths = Set.Transient.of();
    private Set.Transient<P> removedPaths = Set.Transient.of();

    public BUEnv(Function2<P, P, Integer> compare) {
        this(compare, SetMultimap.Transient.of());
    }

    BUEnv(Function2<P, P, Integer> compare, SetMultimap.Transient<SpacedName, P> paths) {
        this.compare = compare;
        this.paths = paths;
    }

    public java.util.Set<SpacedName> nameSet() {
        return paths.keySet();
    }

    public java.util.Collection<P> pathSet() {
        return paths.values();
    }

    SetMultimap<SpacedName, P> paths() {
        return paths;
    }

    public Set.Immutable<P> get(SpacedName name) {
        return paths.get(name);
    }

    public void apply(BUChanges<S, L, O, P> changes) throws InterruptedException {
        for(P path : changes.removedPaths()) {
            removePath(path);
        }
        for(P path : changes.addedPaths()) {
            addPath(path);
        }
    }

    private void removePath(P oldPath) throws InterruptedException {
        final SpacedName name = oldPath.getDeclaration().getSpacedName();
        for(P path : paths.get(name)) {
            if(paths.__remove(name, path)) {
                removedPaths.__insert(path);
            }
        }
    }

    private void addPath(P newPath) throws InterruptedException {
        final SpacedName name = newPath.getDeclaration().getSpacedName();
        for(P path : paths.get(name)) {
            final Integer result = compare.apply(newPath, path);
            if(result != null) {
                // paths are comparable
                if(result < 0) {
                    // the candidate is smaller than an earlier selected path
                    paths.__remove(name, path);
                    addedPaths.__remove(path);
                    removedPaths.__insert(path);
                }
                if(result > 0) {
                    // the candidate is larger than an earlier selected path
                    return;
                }
            }
        }
        // there are no smaller pre-existing paths
        if(paths.__insert(name, newPath)) {
            addedPaths.__insert(newPath);
        }
    }

    public BUChanges<S, L, O, P> commit() throws InterruptedException {
        final BUChanges<S, L, O, P> changes =
                new BUChanges<>(addedPaths.freeze(), removedPaths.freeze());
        addedPaths = Set.Transient.of();
        removedPaths = Set.Transient.of();
        return changes;
    }

    public boolean hasChanges() {
        return !addedPaths.isEmpty() || !removedPaths.isEmpty();
    }

    public void write(String prefix, Writer out) throws IOException {
        out.append(prefix + paths.keySet().size() + " names, " + paths.size() + " paths\n");
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