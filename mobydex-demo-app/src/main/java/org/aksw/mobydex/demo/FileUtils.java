package org.aksw.mobydex.demo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.apache.jena.atlas.RuntimeIOException;

public class FileUtils {
    public static void write(String path, Consumer<OutputStream> consumer) {
        try (OutputStream out = Files.newOutputStream(Path.of(path))) {
            consumer.accept(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }
}
