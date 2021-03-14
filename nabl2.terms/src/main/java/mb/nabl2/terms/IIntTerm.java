package mb.nabl2.terms;

public interface IIntTerm extends ITerm {

    int getValue();

    @Override
    IIntTerm withAttachments(IAttachments value);

}
