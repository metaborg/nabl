package meta.flowspec.java.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.metaborg.meta.nabl2.controlflow.impl.ControlFlowGraph;
import org.metaborg.meta.nabl2.controlflow.terms.CFGNode;
import org.metaborg.meta.nabl2.scopegraph.terms.ImmutableNamespace;
import org.metaborg.meta.nabl2.scopegraph.terms.ImmutableOccurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Namespace;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.OccurrenceIndex;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.collections.IRelation2;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.pcollections.Empty;
import org.pcollections.HashTreePSet;
import org.pcollections.PMap;
import org.pcollections.PSet;

import com.google.common.collect.ImmutableClassToInstanceMap;

import meta.flowspec.java.Pair;
import meta.flowspec.java.interpreter.TransferFunction;
import meta.flowspec.java.lattice.CompleteLattice;
import meta.flowspec.java.lattice.Lattice;
import meta.flowspec.java.pcollections.MapSetPRelation;
import meta.flowspec.java.pcollections.PRelation;

public abstract class MFP2 {
    public static void intraProcedural(
            ControlFlowGraph<CFGNode> cfg,
            PMap<String, Metadata> propMetadata,
            PRelation<String, String> propDependsOn,
            PMap<String, PMap<Integer, TransferFunction>> transferFuns,
            @SuppressWarnings("rawtypes") Map<String, Type> types) {
        // TODO: statically check for cycles in property dependencies in FlowSpec
        List<String> propTopoOrder = MapSetPRelation.topoSort(propDependsOn).get();
        Collections.reverse(propTopoOrder);

        for (String prop : propTopoOrder) {
            final Metadata metadata = propMetadata.get(prop);

            if (metadata.dir() == Metadata.Direction.FlowInsensitive) {
                solveFlowInsensitiveProperty(cfg, prop);
            }

            // Phase 1: initialisation
            PMap<Integer,TransferFunction> tf = transferFuns.get(prop);

            for (CFGNode n : cfg.getAllCFGNodes()) {
                setDataFlowProperty(cfg, n, prop, metadata.lattice().bottom());
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
                    Object afterFromTF = tf.get(getTFNumber(cfg, from, prop)).call(from);
                    Object beforeToTF = getDataFlowProperty(cfg, to, prop);
                    // TODO: use nlte instead of !lte
                    if (!metadata.lattice().lte(afterFromTF, beforeToTF)) {
                        setDataFlowProperty(cfg, to, prop, metadata.lattice().lub(beforeToTF, afterFromTF));
                        workList = workList.plus(to);
                    }
                }
            }

            // Phase 3: Result calculation
            for (CFGNode n : cfg.getAllCFGNodes()) {
                // save pre-TF results
                setDataFlowProperty(cfg, n, "Pre-" + prop, getDataFlowProperty(cfg, n, prop));
                // put post-TF results in property name
                setDataFlowProperty(cfg, n, prop, tf.get(getTFNumber(cfg, n, prop)).call(n));
            }
        }

    }

    private static void solveFlowInsensitiveProperty(ControlFlowGraph<CFGNode> cfg, String prop) {
        // TODO Auto-generated method stub
        
    }

    private static void setDataFlowProperty(ControlFlowGraph<CFGNode> cfg, CFGNode n, String prop, Object value) {
        // TODO Auto-generated method stub
        
    }

    private static Object getDataFlowProperty(ControlFlowGraph<CFGNode> cfg, CFGNode n, String prop) {
        return cfg.getDecls().get(n, prop);
    }

    private static int getTFNumber(ControlFlowGraph<CFGNode> cfg, CFGNode n, String prop) {
        return cfg.getDecls().get(n, "TF").get(prop);
    }
}
