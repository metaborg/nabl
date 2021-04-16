package mb.statix.constraints.messages;

import java.io.Serializable;

public enum MessageKind implements Serializable {

    ERROR {
        @Override public String toString() {
            return "Error";
        }
    },

    WARNING {
        @Override public String toString() {
            return "Warning";
        }
    },

    NOTE {
        @Override public String toString() {
            return "Note";
        }
    },

    IGNORE {
        @Override public String toString() {
            return "Ignore";
        }
    };

    boolean isWorseThan(MessageKind other) {
        return compareTo(other) < 0;
    }

}
