/**
 * Implementation of scope graph representation and resolution algorithm based on the presentations in:
 * 
 * <ul>
 * <li>Antwerpen, Hendrik van, Pierre Néron, Andrew Tolmach, Eelco Visser, and Guido Wachsmuth. “A Constraint Language
 * for Static Semantic Analysis Based on Scope Graphs.” In Proceedings of the 2016 ACM SIGPLAN Workshop on Partial
 * Evaluation and Program Manipulation, 49–60. PEPM ’16. New York, NY, USA: ACM, 2016.
 * https://doi.org/10.1145/2847538.2847543.
 * <li>Antwerpen, Hendrik van, Casper Bach Poulsen, Arjen Rouvoet, and Eelco Visser. “Scopes As Types.” Proc. ACM
 * Program. Lang. 2, no. OOPSLA (October 2018): 114:1-114:30. https://doi.org/10.1145/3276484.
 * <li>Rouvoet, Arjen, Hendrik van Antwerpen, Casper Bach Poulsen, Robbert Krebbers, and Eelco Visser. “Knowing When to
 * Ask: Sound Scheduling of Name Resolution in Type Checkers Derived from Declarative Specifications.” Proceedings of
 * the ACM on Programming Languages 4, no. OOPSLA (November 13, 2020): 180:1-180:28. https://doi.org/10.1145/3428248.
 * </p>
 */
package mb.scopegraph.oopsla20;
