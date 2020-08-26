package grakn.common.parameters;

public abstract class Options<PARENT extends Options, SELF extends Options> {

    public static final boolean DEFAULT_INFER = true;
    public static final boolean DEFAULT_EXPLAIN = false;
    public static final int DEFAULT_BATCH_SIZE = 50;

    private PARENT parent;
    private Boolean infer = null;
    private Boolean explain = null;
    private Integer batchSize = null;

    abstract SELF getThis();

    public SELF parent(PARENT parent) {
        this.parent = parent;
        return getThis();
    }

    public Boolean infer() {
        if (infer != null) {
            return infer;
        } else if (parent != null) {
            return parent.infer();
        } else {
            return DEFAULT_INFER;
        }
    }

    public SELF infer(boolean infer) {
        this.infer = infer;
        return getThis();
    }

    public Boolean explain() {
        if (explain != null) {
            return explain;
        } else if (parent != null) {
            return parent.explain();
        } else {
            return DEFAULT_EXPLAIN;
        }
    }

    public SELF explain(boolean explain) {
        this.explain = explain;
        return getThis();
    }

    public Integer batchSize() {
        if (batchSize != null) {
            return batchSize;
        } else if (parent != null) {
            return parent.batchSize();
        } else {
            return DEFAULT_BATCH_SIZE;
        }
    }

    public SELF batchSize(int batchSize) {
        this.batchSize = batchSize;
        return getThis();
    }

    public static class Database extends Options<Options, Database> {

        @Override
        Database getThis() {
            return this;
        }

        public Database parent(Options parent) {
            throw new IllegalArgumentException("parent");
        }
    }

    public static class Session extends Options<Database, Session> {

        @Override
        Session getThis() {
            return this;
        }
    }

    public static class Transaction extends Options<Session, Transaction> {

        @Override
        Transaction getThis() {
            return this;
        }
    }

    public static class Query extends Options<Transaction, Query> {

        @Override
        Query getThis() {
            return this;
        }
    }
}
