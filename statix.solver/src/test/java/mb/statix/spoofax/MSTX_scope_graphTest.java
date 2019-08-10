package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.modular.util.test.TestUtil.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.incremental.strategy.NonIncrementalStrategy;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.Module;
import mb.statix.modular.solver.Context;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spec.Spec;
import mb.statix.taico.solver.MSolverResult;

public class MSTX_scope_graphTest {
    protected static MSTX_scope_graph stratego = new MSTX_scope_graph();
    protected IModule root;
    protected MSolverResult result;
    protected Context context;
    protected ITerm noRelation;
    
    @Before
    public void setUp() throws Exception {
        Spec spec = createSpec(noRelation = label("%"));
        context = Context.initialContext(new NonIncrementalStrategy(), spec);
        root = Module.topLevelModule("root");
        result = MSolverResult.of(root.getCurrentState(), new ArrayList<>(), new HashMap<>(), new HashMap<>());
    }

    @Test
    public void testCallIContextITermListOfITerm() {
        Scope SG = scope(root, "G");
        
        IModule A = createChild(root, "A", SG);
        Scope SA = scope(A, "A");
        
        IModule A1 = createChild(A, "A1", SG, SA);
        Scope SA1 = scope(A1, "A1");
        
        IModule A2 = createChild(A, "A2", SG, SA);
        Scope SA2 = scope(A2, "A2");
        
        IModule B = createChild(root, "B", SG);
        Scope SB = scope(B, "B");
        
        edge(A, SA, SG, "P");
        edge(B, SB, SG, "P");
        edge(A1, SA1, SA, "P");
        edge(A2, SA2, SA, "P");
        edge(B, SB, SA, "I");
        
        data(A1, SA, "Class{A1}", ":");
        data(A2, SA, "Class{A2}", ":");
        data(B, SB, "Class{B}", ":");
        
        String str = call(null);
        System.out.println(str);
    }
    
    protected String call(String file) {
        Optional<? extends ITerm> callResult;
        try {
            callResult = stratego.call(null, file == null ? B.newInt(0) : B.newString(file), list(B.newBlob(result)));
        } catch (InterpreterException e) {
            throw new RuntimeException(e);
        }
        
        return M.stringValue().match(callResult.get()).get();
    }
    
    protected static Scope scope(IModule module, String name) {
        return module.getCurrentState().freshScope(name, null);
    }
    
    protected void edge(IModule module, Scope from, Scope to, String label) {
        module.getScopeGraph().addEdge(from, label(label), to);
    }
    
    protected void data(IModule module, Scope from, String to, String label) {
        //TODO Use something else than a label
        module.getScopeGraph().addDatum(from, label(label), label(to));
    }
}
