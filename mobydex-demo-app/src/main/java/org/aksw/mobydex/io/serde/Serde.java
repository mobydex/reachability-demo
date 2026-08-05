package org.aksw.mobydex.io.serde;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Serde<T> {
    void write(OutputStream out, T item) throws IOException;
    T read(InputStream in) throws IOException;
}
