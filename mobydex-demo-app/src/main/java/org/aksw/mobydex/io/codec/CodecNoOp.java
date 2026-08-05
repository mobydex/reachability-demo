package org.aksw.mobydex.io.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CodecNoOp implements Codec {
    @Override public InputStream decode(InputStream in) throws IOException { return in; }
    @Override public OutputStream encode(OutputStream out) throws IOException { return out; }
}
