package mb.nabl2.terms;

public interface IStringTerm extends ITerm {

    String getValue();

    @Override
    IStringTerm withAttachments(IAttachments value);

}
