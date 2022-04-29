package mb.nabl2.terms;

public interface ITermVar extends ITerm, IListTerm, Comparable<ITermVar> {

    String getResource();

    String getName();

    @Override ITermVar withAttachments(IAttachments value);

    @Override default int compareTo(ITermVar other) {
        int c = 0;
        if(c == 0) {
            c = getResource().compareTo(other.getResource());
        }
        if(c == 0) {
            c = getName().compareTo(other.getName());
        }
        return c;
    }

    @Override default ITerm.Tag termTag() {
        return ITerm.Tag.ITermVar;
    }

    @Override default IListTerm.Tag listTermTag() {
        return IListTerm.Tag.ITermVar;
    }

}