// @formatter:off
@Value.Style(
    typeAbstract = { "*" },
    get = { "is*", "get*" },
    with = "with*",
    defaults = @Value.Immutable(builder = false, copy = true),
    // prevent generation of javax.annotation.*; bogus entry, because empty list = allow all
    allowedClasspathAnnotations = {Override.class}
)
// @formatter:on
@DefaultQualifier(NonNull.class)
package mb.nabl2;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.immutables.value.Value;