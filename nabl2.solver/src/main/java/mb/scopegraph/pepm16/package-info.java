/**
 * Implementation of the scope graph resolution algorithm presented in:
 * 
 * <p>
 * Antwerpen, Hendrik van, Pierre Néron, Andrew Tolmach, Eelco Visser, and Guido Wachsmuth. “A Constraint Language for
 * Static Semantic Analysis Based on Scope Graphs.” In Proceedings of the 2016 ACM SIGPLAN Workshop on Partial
 * Evaluation and Program Manipulation, 49–60. PEPM ’16. New York, NY, USA: ACM, 2016.
 * https://doi.org/10.1145/2847538.2847543.
 * </p>
 * 
 * Ideally this code was part of the scopegraph project. However, the code is not thoroughly parametrized and this would
 * require a dependency from the scopegraph project on nabl2.terms. Therefore this package is part of the nabl2.solver
 * project instead.
 */
// @formatter:off
@Value.Style(
    typeAbstract = { "A*" },
    typeImmutable = "*",
    get = { "is*", "get*" },
    with = "with*",
    defaults = @Value.Immutable(builder = false, copy = true),
    // prevent generation of jakarta.annotation.*; bogus entry, because empty list = allow all
    allowedClasspathAnnotations = {Override.class}
)
// @formatter:on
package mb.scopegraph.pepm16;

import org.immutables.value.Value;