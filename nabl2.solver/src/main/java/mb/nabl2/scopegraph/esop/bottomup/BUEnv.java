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

public class BUEnv<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>> {

    @SuppressWarnings("unused") private static final ILogger logger = LoggerUtils.logger(BUEnv.class);

    private final Function2<P, P, Integer> compare;

    private final SetMultimap.Transient<SpacedName, P> paths = SetMultimap.Transient.of();

    public BUEnv(Function2<P, P, Integer> compare) {
        this.compare = compare;
    }

    public java.util.Set<SpacedName> nameSet() {
        return paths.keySet();
    }

    public java.util.Collection<P> pathSet() {
        return paths.values();
    }

    public Set.Immutable<P> get(SpacedName name) {
        return paths.get(name);
    }

    public BUChanges<S, L, O, P> apply(BUChanges<S, L, O, P> changes) throws InterruptedException {
        //        logger.info("adding {} paths to {} names, {} paths env", paths.size(), env.keySet().size(), env.values().size());
        final Set.Transient<P> addedPaths = Set.Transient.of();
        final Set.Transient<P> removedPaths = Set.Transient.of();
        for(P path : changes.removedPaths()) {
            removePath(path, removedPaths);
        }
        for(P path : changes.addedPaths()) {
            addPath(path, addedPaths, removedPaths);
        }
        return new BUChanges<>(addedPaths.freeze(), removedPaths.freeze());
    }

    private void removePath(P oldPath, Set.Transient<P> removed) throws InterruptedException {
        final SpacedName name = oldPath.getDeclaration().getSpacedName();
        //        logger.info("adding path to {} env", env.get(name).size());
        for(P path : paths.get(name)) {
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            if(paths.__remove(name, path)) {
                removed.__insert(path);
            }
        }
    }

    private void addPath(P newPath, Set.Transient<P> added, Set.Transient<P> removed) throws InterruptedException {
        final SpacedName name = newPath.getDeclaration().getSpacedName();
        //        logger.info("adding path to {} env", env.get(name).size());
        for(P path : paths.get(name)) {
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            final Integer result = compare.apply(newPath, path);
            if(result != null) {
                // paths are comparable
                if(result < 0) {
                    // the candidate is smaller than an earlier selected path
                    paths.__remove(name, path);
                    removed.__insert(path);
                }
                if(result > 0) {
                    // the candidate is larger than an earlier selected path
                    return;
                }
            }
        }
        // there are no smaller selected paths
        if(paths.__insert(name, newPath)) {
            added.__insert(newPath);
        }
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