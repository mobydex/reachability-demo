package org.aksw.mobydex.io.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Codec {
    InputStream decode(InputStream in) throws IOException;
    OutputStream encode(OutputStream out) throws IOException;
}
