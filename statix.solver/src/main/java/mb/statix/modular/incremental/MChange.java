package mb.statix.modular.incremental;

import static mb.nabl2.terms.matching.TermMatch.M;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.ModuleManager;
import mb.statix.solver.ISolverResult;

public class MChange {
    public static enum ChangeEnum {
        ADDED,
        REMOVED,
        CHANGED,
        UNCHANGED
    }
    
    private final ChangeEnum changeType;
    private final String module;
    private final ITerm ast;
    private final ISolverResult oldAnalysis;
    
    public MChange(ChangeEnum changeType, String module, ITerm ast, ISolverResult oldAnalysis) {
        this.changeType = changeType;
        this.module = module;
        this.ast = ast;
        this.oldAnalysis = oldAnalysis;
    }
    
    public String getModule() {
        return module;
    }
    
    public IModule getModule(ModuleManager manager) {
        return manager.getModule(module);
    }

    public ChangeEnum getChangeType() {
        return changeType;
    }

    public ITerm getAst() {
        return ast;
    }

    public ISolverResult getOldAnalysis() {
        return oldAnalysis;
    }

    private static MChange added(String module, ITerm ast) {
        return new MChange(ChangeEnum.ADDED, module, ast, null);
    }
    
    private static MChange removed(String module, ISolverResult oldAnalysis) {
        return new MChange(ChangeEnum.REMOVED, module, null, oldAnalysis);
    }
    
    private static MChange changed(String module, ITerm ast, ISolverResult oldAnalysis) {
        return new MChange(ChangeEnum.CHANGED, module, ast, oldAnalysis);
    }
    
    private static MChange cached(String module, ISolverResult oldAnalysis) {
        return new MChange(ChangeEnum.UNCHANGED, module, null, oldAnalysis);
    }
    
    public static IMatcher<MChange> matcher() {
        return M.cases(
                M.appl2("MAdded", M.stringValue(), M.term(),
                        (t, module, ast) -> added(module, ast)),
                M.appl2("MRemoved", M.stringValue(), analysisMatcher(),
                        (t, module, analysis) -> removed(module, analysis)),
                M.appl2("MUnchanged", M.stringValue(), analysisMatcher(),
                        (t, module, analysis) -> cached(module, analysis)),
                M.appl3("MChanged", M.stringValue(), M.term(), analysisMatcher(),
                        (t, module, ast, analysis) -> changed(module, ast, analysis)));
    }
    
    private static IMatcher<ISolverResult> analysisMatcher() {
        return M.cases(
                M.appl1("MModuleAnalysis", M.blobValue(ISolverResult.class), (t, r) -> r),
                M.appl2("MProjectAnalysis", M.term(), M.blobValue(ISolverResult.class), (t, s, r) -> r)
        );
    }
}
