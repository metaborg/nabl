package statix.cli.incremental.changes;

import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

@FunctionalInterface
public interface IIncrementalChange {
    ISpoofaxParseUnit apply(ISpoofaxParseUnit unit);
}
