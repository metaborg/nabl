@Value.Style(
// @formatter:off
    typeAbstract = { "*" },
    get = { "is*", "get*" },
    with = "with*",
    defaults = @Value.Immutable(builder = false)
    // @formatter:on
)
package mb.nabl2;

import org.immutables.value.Value;