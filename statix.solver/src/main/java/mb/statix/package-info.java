@Value.Style(
// @formatter:off
    typeAbstract = { "A*" },
    typeImmutable = "*",
    get = { "is*", "get*" },
    with = "with*",
    defaults = @Value.Immutable(builder = true, prehash = true)
    // @formatter:on
)
package mb.statix;

import org.immutables.value.Value;