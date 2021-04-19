package mb.scopegraph.pepm16.bottomup;

enum BUEnvKind {
    VISIBLE { // top-level and import paths are ordered
    },
    REACHABLE { // top-level paths are unordered, import paths are ordered
    }
}