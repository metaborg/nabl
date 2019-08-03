package mb.statix.taico.scopegraph.reference;

import java.util.Optional;

public class ModuleDelayException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String module;
    private final String requester;

    public ModuleDelayException(String module) {
        super("Incomplete module \"" + module + "\"");
        this.module = module;
        this.requester = null;
    }
    
    public ModuleDelayException(String requester, String module) {
        super("Module \"" + module + "\" is incomplete or inaccessible from module \"" + requester + "\"");
        this.module = module;
        this.requester = requester;
    }
    
    /**
     * @return
     *      the module that is the cause of this exception
     */
    public String getModule() {
        return module;
    }
    
    public Optional<String> getRequester() {
        return Optional.of(requester);
    }
}
