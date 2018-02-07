package nl.trivento.fastdata.travelclear.routes.entities.serialization;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Session extends AutoCloseable {
    Transaction startTransaction(boolean readwrite) throws IOException;

    @Override
    void close();

    default <T> T readOp(Function<Transaction, T> run) throws IOException {
        try (Transaction tx = startTransaction(true)) {
            return run.apply(tx);
        }
    }

    default void writeOp(Consumer<Transaction> run) throws IOException {
        try (Transaction tx = startTransaction(true)) {
            run.accept(tx);
            tx.commit();
        }
    }
}
