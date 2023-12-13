package mb.statix.generator.nodes;

import jakarta.annotation.Nullable;

/**
 * An element in the search tree.
 */
public interface SearchElement {

    /**
     * Gets a description of the search element.
     *
     * @return a description
     */
    String desc();

    /**
     * Gets the parent node of the search element.
     *
     * @return the parent node; or {@code null} when this is the root node
     */
    @Nullable
    SearchNode<?> parent();

}