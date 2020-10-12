package mb.nabl2.terms;

public interface ITermVar extends ITerm, IListTerm {

    String getResource();

    String getName();

    @Override
    ITermVar withAttachments(IAttachments value);

}
