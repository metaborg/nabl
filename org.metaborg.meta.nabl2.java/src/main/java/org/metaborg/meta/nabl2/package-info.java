@Value.Style(
    // @formatter:off
    typeAbstract = { "*" },
    get = { "is*", "get*" },
    with = "with*",
    defaults = @Value.Immutable(builder = false, prehash = true)
    // @formatter:on
)
package org.metaborg.meta.nabl2;

import org.immutables.value.Value;