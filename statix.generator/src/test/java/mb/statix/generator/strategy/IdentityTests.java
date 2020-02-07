package mb.statix.generator.strategy;

import mb.statix.generator.DefaultSearchContext;
import mb.statix.generator.SearchContext;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.spec.Spec;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


/**
 * Tests the {@link Identity} strategy.
 */
public final class IdentityTests {

    private SearchContext createContext() {
        return new DefaultSearchContext(Spec.of());
    }

    @Test
    public void returnsInputUnchanged() {
        // Arrange
        SearchContext ctx = createContext();
        SearchNode<String> node = new SearchNode<>(0, "a", null, "init");
        Identity<String> strategy = new Identity<>();

        // Act
        SearchNodes<String> result = strategy.doApply(ctx, node);

        // Assert
        assertEquals(Arrays.asList(node), result.nodes().collect(Collectors.toList()));
    }

}
