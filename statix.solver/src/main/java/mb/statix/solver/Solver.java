package mb.statix.solver;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class Solver {
    @SuppressWarnings("unused") private static final ILogger logger = LoggerUtils.logger(Solver.class);

    private Solver() {
    }

    public static Config solve(Config config, boolean exhaustive) throws InterruptedException {
        // set-up
        final Set<IConstraint> constraints = Sets.newHashSet(config.getConstraints());
        State state = config.state();

        // fixed point
        boolean progress = true;
        outer: while(progress) {
            progress = false;
            final Iterator<IConstraint> it = constraints.iterator();
            final List<IConstraint> newConstraints = Lists.newArrayList();
            while(it.hasNext()) {
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }
                final IConstraint constraint = it.next();
                Optional<Config> maybeResult = constraint.solve(state);
                if(maybeResult.isPresent()) {
                    it.remove();
                    progress = true;
                    final Config result = maybeResult.get();
                    state = result.state();
                    // FIXME update properties in state
                    newConstraints.addAll(result.getConstraints());
                    if(!exhaustive && state.isErroneous()) {
                        break outer;
                    }
                }
            }
            constraints.addAll(newConstraints);
            newConstraints.clear();
        }

        // return
        return Config.of(state, constraints);
    }

}