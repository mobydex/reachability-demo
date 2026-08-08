package org.aksw.mobydex.demo.appstate;

public sealed interface LoadingState<T, V> {
    T id();
    // V payload();
    // Throwable error();

    // public record Enqeued<T>(T id) implements LoadingState<T> {}
    public record Loading<T, V>(T id) implements LoadingState<T, V> {}
    public record Loaded<T, V>(T id, V payload) implements LoadingState<T, V> {}
    public record Failed<T, V>(T id, Throwable error) implements LoadingState<T, V> {}

    public static <T, V> Loading<T, V> loading(T id) { return new Loading<>(id); }
    public static <T, V> Loaded<T, V> loaded(T id, V payload) { return new Loaded<>(id, payload); }
    public static <T, V> Failed<T, V> failed(T id, Throwable error) { return new Failed<>(id, error); }
}
