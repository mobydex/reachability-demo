package org.aksw.mobydex.io.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

public class CodecBZip2 implements Codec {
    private static final CodecBZip2 INSTANCE = new CodecBZip2();

    public static CodecBZip2 get() { return INSTANCE; }

    @Override
    public InputStream decode(InputStream in) throws IOException {
        return new BZip2CompressorInputStream(in, true);
    }

    @Override
    public OutputStream encode(OutputStream out) throws IOException {
        return new BZip2CompressorOutputStream(out);
    }
}
