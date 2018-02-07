package nl.trivento.fastdata.travelclear.routes.entities;

import java.util.function.Function;

public interface Reference<K, T> {
    K id();
    T resolve();

    static <K, T> Reference<K, T> direct(K id, T obj) {
        return new Reference<K, T>() {
            @Override
            public K id() {
                return id;
            }

            @Override
            public T resolve() {
                return obj;
            }
        };
    }

    static <K, T> Reference<K, T> empty(K id) {
        return new Reference<K, T>() {
            @Override
            public K id() {
                return id;
            }

            @Override
            public T resolve() {
                return null;
            }
        };
    }

    static <K, T> Reference<K, T> lazy(K id, Function<K, T> eval) {
        return new Reference<K, T>() {
            private T evaluated;
            private boolean isEvaluated;

            @Override
            public K id() {
                return id;
            }

            @Override
            public T resolve() {
                if (!isEvaluated) {
                    evaluated = eval.apply(id);
                }
                return evaluated;
            }
        };
    }
}
