package mb.statix.constraints.messages;

public enum MessageKind {

    ERROR {
        @Override public String toString() {
            return "Error";
        }
    },

    WARNING {
        @Override public String toString() {
            return "Error";
        }
    },

    NOTE {
        @Override public String toString() {
            return "Error";
        }
    };

    boolean isWorseThan(MessageKind other) {
        return compareTo(other) < 0;
    }

}