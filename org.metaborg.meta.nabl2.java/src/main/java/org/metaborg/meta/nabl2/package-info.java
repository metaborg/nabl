@Value.Style(
    // @formatter:off
    typeAbstract = { "*" },
    typeImmutable = "Immutable*",
    get = { "is*", "get*" },
    allParameters = true,
    with = "set*",
    defaults = @Value.Immutable(builder = false, prehash = true)
    // @formatter:on
)
package org.metaborg.meta.nabl2;

import org.immutables.value.Value;