package mb.nabl2.scopegraph.esop.bottomup;

enum BUEnvKind {
    VISIBLE { // top-level and import paths are ordered
    },
    REACHABLE { // top-level paths are unordered, import paths are ordered
    }
}