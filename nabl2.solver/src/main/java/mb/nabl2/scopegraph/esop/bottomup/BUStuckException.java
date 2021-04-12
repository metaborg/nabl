package mb.nabl2.scopegraph.esop.bottomup;

import java.util.Set;

import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.tuple.Tuple3;

import mb.nabl2.scopegraph.StuckException;

@SuppressWarnings("rawtypes")
public class BUStuckException extends StuckException {

    private static final long serialVersionUID = 1L;

    private final Set<? extends BUEnvKey> environments;
    private final Set<? extends Tuple3<? extends BUEnvKey, ?, ? extends BUEnvKey>> edges;
    private final Set<? extends Tuple3<? extends BUEnvKey, ? extends Tuple2<?, ?>, ? extends BUEnvKey>> imports;

    public BUStuckException(Set<? extends BUEnvKey> environments,
            Set<? extends Tuple3<? extends BUEnvKey, ?, ? extends BUEnvKey>> edges,
            Set<? extends Tuple3<? extends BUEnvKey, ? extends Tuple2<?, ?>, ? extends BUEnvKey>> imports) {
        this.environments = environments;
        this.edges = edges;
        this.imports = imports;
    }

    @Override public String getMessage() {
        final StringBuilder sb = new StringBuilder();
        sb.append("stuck:").append('\n');
        sb.append("| environments:").append('\n');
        for(BUEnvKey env : environments) {
            sb.append(" * ").append(env).append('\n');
        }
        sb.append("| edges:").append('\n');
        for(Tuple3<? extends BUEnvKey, ?, ? extends BUEnvKey> edge : edges) {
            sb.append(" * ").append(edge._1()).append(" -").append(edge._2()).append("-> ").append(edge._3())
                    .append('\n');
        }
        sb.append("| imports:").append('\n');
        for(Tuple3<? extends BUEnvKey, ? extends Tuple2<?, ?>, ? extends BUEnvKey> imp : imports) {
            sb.append(" * ").append(imp._1()).append(" =").append(imp._2()._1()).append("/").append(imp._2()._2())
                    .append("=> ").append(imp._3()).append('\n');
        }
        return sb.toString();
    }

}