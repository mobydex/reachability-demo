package org.aksw.mobydex.io.serde;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.rowset.RowSetReader;
import org.apache.jena.riot.rowset.RowSetReaderFactory;
import org.apache.jena.riot.rowset.RowSetReaderRegistry;
import org.apache.jena.riot.rowset.RowSetWriter;
import org.apache.jena.riot.rowset.RowSetWriterFactory;
import org.apache.jena.riot.rowset.RowSetWriterRegistry;
import org.apache.jena.sparql.exec.RowSet;

public class SerdeRowSet implements Serde<RowSet> {
    private final Lang lang;
    private final RowSetWriter writer;
    private final RowSetReader reader;

    protected SerdeRowSet(Lang lang, RowSetWriter writer, RowSetReader reader) {
        super();
        this.lang = lang;
        this.writer = writer;
        this.reader = reader;
    }

    public Lang getLang() {
        return lang;
    }

    public static SerdeRowSet of(Lang lang) {
        RowSetWriterFactory wf = RowSetWriterRegistry.getFactory(lang);
        RowSetWriter w = wf.create(lang);
        RowSetReaderFactory wr = RowSetReaderRegistry.getFactory(lang);
        RowSetReader r = wr.create(lang);
        return new SerdeRowSet(lang, w, r);
    }

    @Override
    public void write(OutputStream out, RowSet item) throws IOException {
        writer.write(out, item, null);
    }

    @Override
    public RowSet read(InputStream in) throws IOException {
        return reader.read(in, null);
    }
}
