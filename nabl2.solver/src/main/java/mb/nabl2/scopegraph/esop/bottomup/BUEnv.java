package mb.nabl2.scopegraph.esop.bottomup;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

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

    private static final ILogger logger = LoggerUtils.logger(BUEnv.class);

    private final Function2<P, P, Integer> compare;

    private final SetMultimap.Transient<SpacedName, P> env = SetMultimap.Transient.of();

    public BUEnv(Function2<P, P, Integer> compare) {
        this.compare = compare;
    }

    public java.util.Set<SpacedName> nameSet() {
        return env.keySet();
    }

    public java.util.Collection<P> pathSet() {
        return env.values();
    }

    public Set.Immutable<P> get(SpacedName name) {
        return env.get(name);
    }

    public Set.Immutable<P> addAll(Collection<P> paths) throws InterruptedException {
        //        logger.info("adding {} paths to {} names, {} paths env", paths.size(), env.keySet().size(), env.values().size());
        final Set.Transient<P> added = Set.Transient.of();
        for(P path : paths) {
            if(add(path)) {
                added.__insert(path);
            }
        }
        return added.freeze();
    }

    public boolean add(P newPath) throws InterruptedException {
        final SpacedName name = newPath.getDeclaration().getSpacedName();
        //        logger.info("adding path to {} env", env.get(name).size());
        for(P path : env.get(name)) {
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            final Integer result = compare.apply(newPath, path);
            if(result != null) {
                // paths are comparable
                if(result < 0) {
                    // the candidate is smaller than an earlier selected path
                    env.__remove(name, path);
                }
                if(result > 0) {
                    // the candidate is larger than an earlier selected path
                    return false;
                }
            }
        }
        // there are no smaller selected paths
        return env.__insert(name, newPath);
    }

    public void write(String prefix, Writer out) throws IOException {
        for(SpacedName name : env.keySet()) {
            out.append(prefix + name + ":\n");
            for(P path : env.get(name)) {
                out.append(prefix + " - " + path + "\n");
            }
        }
    }

}