@Value.Style(
// @formatter:off
    typeAbstract = { "A*" },
    typeImmutable = "*",
    get = { "is*", "get*" },
    with = "with*",
    defaults = @Value.Immutable(builder = true)
    // @formatter:on
)
package mb.statix;

import org.immutables.value.Value;