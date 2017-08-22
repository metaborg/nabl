package meta.flowspec.java.solver;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.metaborg.meta.nabl2.controlflow.impl.ControlFlowGraph;
import org.metaborg.meta.nabl2.controlflow.terms.CFGNode;
import org.metaborg.meta.nabl2.util.collections.IRelation2;
import org.pcollections.HashTreePSet;
import org.pcollections.PMap;
import org.pcollections.PSet;

import meta.flowspec.java.interpreter.TransferFunction;
import meta.flowspec.java.pcollections.MapSetPRelation;
import meta.flowspec.java.pcollections.PRelation;

public abstract class MFP2 {
    public static void intraProcedural(
            ControlFlowGraph<CFGNode> cfg,
            PMap<String, Metadata> propMetadata,
            PRelation<String, String> propDependsOn,
            PMap<String, TransferFunction[]> transferFuns,
            Map<String, Type> types) {
        // TODO: statically check for cycles in property dependencies in FlowSpec
        List<String> propTopoOrder = MapSetPRelation.topoSort(propDependsOn).get();
        Collections.reverse(propTopoOrder);

        for (String prop : propTopoOrder) {
            final Metadata metadata = propMetadata.get(prop);

            if (metadata.dir() == Metadata.Direction.FlowInsensitive) {
                solveFlowInsensitiveProperty(cfg, prop);
            }

            // Phase 1: initialisation
            TransferFunction[] tf = transferFuns.get(prop);

            for (CFGNode n : cfg.getAllCFGNodes()) {
                cfg.setProperty(n, prop, metadata.lattice().bottom());
                // No need to set a different value for the start node, since the rule for the start node will result
                //  in that value, which will be propagated Phase 2. 
            }

            // Phase 2: Fixpoint iteration
            final IRelation2<CFGNode, CFGNode> edges;
            switch (metadata.dir()) {
                case Forward: {
                    edges = cfg.getDirectEdges();
                    break;
                }
                case Backward: {
                    edges = cfg.getDirectEdges();
                    break;
                }
                default: {
                    throw new RuntimeException("Unreachable: Dataflow property direction enum has unexpected value");
                }
            }

            // TODO: start at start node (or end node in case of Backward dir)
            PSet<CFGNode> workList = HashTreePSet.from(edges.keySet().asSet());

            while (!workList.isEmpty()) {
                final CFGNode from = workList.iterator().next();
                workList = workList.minus(from);
                for (CFGNode to : edges.get(from)) {
                    Object afterFromTF = cfg.getTFAppl(from, prop).call(tf, from);
                    Object beforeToTF = cfg.getProperty(to, prop);
                    // TODO: use nlte instead of !lte
                    if (!metadata.lattice().lte(afterFromTF, beforeToTF)) {
                        cfg.setProperty(to, prop, metadata.lattice().lub(beforeToTF, afterFromTF));
                        workList = workList.plus(to);
                    }
                }
            }

            // Phase 3: Result calculation
            for (CFGNode n : cfg.getAllCFGNodes()) {
                // save pre-TF results
                cfg.setProperty(n, "Pre-" + prop, cfg.getProperty(n, prop));
                // put post-TF results in property name
                cfg.setProperty(n, prop, cfg.getTFAppl(n, prop).call(tf, n));
            }
        }

    }

    private static void solveFlowInsensitiveProperty(ControlFlowGraph<CFGNode> cfg, String prop) {
        throw new RuntimeException("Unimplemented");
    }
}
