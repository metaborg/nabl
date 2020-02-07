package mb.statix.generator.nodes;

import javax.annotation.Nullable;


public interface SearchElement {

    String desc();

    @Nullable
    SearchNode<?> parent();

}