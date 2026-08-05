package org.aksw.mobydex.io.serde;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;

public class SerdeDataset implements Serde<DatasetGraph> {
    private final RDFFormat rdfFormat;

    protected SerdeDataset(RDFFormat rdfFormat) {
        super();
        this.rdfFormat = rdfFormat;
    }

    public RDFFormat getRDFFormat() {
         return rdfFormat;
    }

    public static SerdeDataset of(RDFFormat rdfFormat) {
        Objects.requireNonNull(rdfFormat);
        return new SerdeDataset(rdfFormat);

        // Note: Could also pin the exact writer at creation time:
        // WriterDatasetRIOTFactory wf = RDFWriterRegistry.getWriterDatasetFactory(rdfFormat);
        // WriterDatasetRIOT w = wf.create(rdfFormat);
    }

    @Override
    public void write(OutputStream out, DatasetGraph dsg) throws IOException {
        RDFDataMgr.write(out, dsg, rdfFormat);
    }

    @Override
    public DatasetGraph read(InputStream in) throws IOException {
        Lang lang = rdfFormat.getLang();
        DatasetGraph dsg = DatasetGraphFactory.create();
        RDFDataMgr.read(dsg, in, lang);
        return dsg;
    }
}
