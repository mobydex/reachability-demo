package org.aksw.mobydex.demo.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.base.Converter;

import org.aksw.commons.io.util.PathUtils;
import org.aksw.mobydex.io.codec.Codec;
import org.aksw.mobydex.io.codec.CodecBZip2;
import org.aksw.mobydex.io.serde.Serde;
import org.aksw.mobydex.io.serde.SerdeDataset;
import org.aksw.mobydex.io.serde.SerdeRowSet;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.jena.atlas.io.IOX;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.TableFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphMapLink;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.graph.GraphReadOnly;

public class FileCaches {
    private static final Converter<Table, RowSet> converter_tableToRowSet = Converter.from(Table::toRowSet, TableFactory::create);

    private static final Converter<Resource, DatasetGraph> converter_resourceToDatasetGraph =
            Converter.from(FileCaches::resourceToDataset, FileCaches::datasetToResource);

    public static Converter<Table, RowSet> getConverterTableToRowSet() {
        return converter_tableToRowSet;
    }

    public static Converter<Resource, DatasetGraph> getConverterResourceToDatasetGraph() {
        return converter_resourceToDatasetGraph;
    }

    public static Table loadTable(FileCache fileCache, List<String> key, Callable<Table> creator) {
        return load(fileCache, key, creator, "table-", ".srj.bz2",
                x -> x, getConverterTableToRowSet(), SerdeRowSet.of(ResultSetLang.RS_JSON), CodecBZip2.get());
    }

    @SuppressWarnings("unchecked")
    public static <T, X> T load(FileCache fileCache, List<String> key, Callable<T> creator, String tmpFilePrefix, String tmpFileSuffix, Function<T, T> readOnlyMaker, Converter<T, X> converter, Serde<X> serde, Codec codec) {
        Cache<List<String>, Object>cache = fileCache.getCache();
        Path cacheBasePath = fileCache.getCacheBasePath();

        T result = (T)cache.get(key, k -> {
            T tmp;
            Path path = PathUtils.resolve(cacheBasePath, key);
            System.err.println("Processing: " + path);
            if (!Files.exists(path)) {
                Path tmpFile;
                try {
                    Path parentPath = path.getParent();
                    if (parentPath != null) {
                        Files.createDirectories(parentPath);
                    }
                    tmpFile = Files.createTempFile(tmpFilePrefix, tmpFileSuffix);
                    tmp = creator.call();
                    // DatasetGraph dsg = resourceToDataset(tmp);
                    X dsg = converter.convert(tmp);
                    IOX.safeWriteOrCopy(path, tmpFile, out -> {
                        try (OutputStream encodedOut = codec.encode(CloseShieldOutputStream.wrap(out))) {
                            serde.write(encodedOut, dsg);
                            // RDFDataMgr.write(encodedOut, dsg, RDFFormat.TRIG_BLOCKS);
                            encodedOut.flush();
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                // DatasetGraph dsg = RDFDataMgr.loadDatasetGraph(path.toString(), Lang.TRIG);
                X dsg;
                try (InputStream in = codec.decode(Files.newInputStream(path))) {
                    dsg = serde.read(in);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                // tmp = datasetToResource(dsg);
                tmp = converter.reverse().convert(dsg);
            }
            // Resource readOnly = readOnly(tmp.getModel()).asRDFNode(tmp.asNode()).asResource();
            T r = readOnlyMaker.apply(tmp);
            return r;
            // return readOnly;
        });
        return result;
    }

    public static Resource makeReadOnly(Resource tmp) {
        Resource readOnly = readOnly(tmp.getModel()).asRDFNode(tmp.asNode()).asResource();
        return readOnly;
    }

    public static Resource loadResource(FileCache fileCache, List<String> key, Callable<Resource> creator) {
        return load(fileCache, key, creator, "resource-", ".trig.bz2",
                x -> x, getConverterResourceToDatasetGraph(), SerdeDataset.of(RDFFormat.TRIG_BLOCKS), CodecBZip2.get());
    }

    public static DatasetGraph resourceToDataset(Resource r) {
        DatasetGraphMapLink dsg = new DatasetGraphMapLink(GraphFactory.createDefaultGraph());
        dsg.addGraph(r.asNode(), r.getModel().getGraph());
        return dsg;
    }

    public static Resource datasetToResource(DatasetGraph dsg) {
        List<Node> graphNodes = Iter.toList(dsg.listGraphNodes());
        if (graphNodes.size() != 1) {
            throw new RuntimeException("Exactly 1 graph expected");
        }
        Node graphNode = graphNodes.getFirst();
        Graph g = dsg.getGraph(graphNode);
        Model m = ModelFactory.createModelForGraph(g);
        Resource r = m.asRDFNode(graphNode).asResource();
        return r;
    }

    public static Model readOnly(Model model) {
        Graph graph = model.getGraph();
        Model result = graph instanceof GraphReadOnly
            ? model
            : ModelFactory.createModelForGraph(new GraphReadOnly(graph));
        return result;
    }
}
