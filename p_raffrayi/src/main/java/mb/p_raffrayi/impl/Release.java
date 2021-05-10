package mb.p_raffrayi.impl;

public class Release extends RuntimeException {

    private static final long serialVersionUID = 5727051324388959596L;

    public static final Release instance = new Release();

    private Release() {
       super("", null, false, false);
    }

    @Override public boolean equals(Object obj) {
        return obj == this;
    }

}
