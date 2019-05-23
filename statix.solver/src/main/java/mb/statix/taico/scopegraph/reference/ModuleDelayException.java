package mb.statix.taico.scopegraph.reference;

public class ModuleDelayException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String module;

    public ModuleDelayException(String module) {
        super("Incomplete module " + module);
        this.module = module;
    }
    
    /**
     * @return
     *      the module that is the cause of this exception
     */
    public String getModule() {
        return module;
    }
}
