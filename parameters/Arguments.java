package grakn.common.parameters;

public class Arguments {

    public static class Session {

        public enum Type {
            DATA(0),
            SCHEMA(1);

            private final int id;
            private final boolean isSchema;

            Type(int id) {
                this.id = id;
                this.isSchema = id == 1;
            }

            public static Arguments.Session.Type of(int value) {
                for (Arguments.Session.Type t : values()) {
                    if (t.id == value) return t;
                }
                return null;
            }

            public int id() {
                return id;
            }

            public boolean isData() { return !isSchema; }

            public boolean isSchema() { return isSchema; }
        }
    }

    public static class Transaction {

        public enum Type {
            READ(0),
            WRITE(1);

            private final int id;
            private final boolean isWrite;

            Type(int id) {
                this.id = id;
                this.isWrite = id == 1;
            }

            public static Arguments.Transaction.Type of(int value) {
                for (Arguments.Transaction.Type t : values()) {
                    if (t.id == value) return t;
                }
                return null;
            }

            public int id() {
                return id;
            }

            public boolean isRead() { return !isWrite; }

            public boolean isWrite() { return isWrite; }
        }
    }
}
