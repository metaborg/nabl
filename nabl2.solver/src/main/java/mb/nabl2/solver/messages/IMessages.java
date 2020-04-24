package mb.nabl2.solver.messages;

import java.util.List;
import java.util.stream.Collectors;

import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageKind;

public interface IMessages {

    interface Immutable extends IMessages {

        List<IMessageInfo> getAll();

        default List<IMessageInfo> getErrors() {
            return getAll().stream().filter(m -> m.getKind().equals(MessageKind.ERROR)).collect(Collectors.toList());
        }

        default List<IMessageInfo> getWarnings() {
            return getAll().stream().filter(m -> m.getKind().equals(MessageKind.WARNING)).collect(Collectors.toList());
        }

        default List<IMessageInfo> getNotes() {
            return getAll().stream().filter(m -> m.getKind().equals(MessageKind.NOTE)).collect(Collectors.toList());
        }

        IMessages.Transient melt();

    }

    interface Transient extends IMessages {

        boolean add(IMessageInfo message);

        boolean addAll(Iterable<? extends IMessageInfo> messages);

        boolean addAll(IMessages.Immutable messages);

        IMessages.Immutable freeze();

    }

}