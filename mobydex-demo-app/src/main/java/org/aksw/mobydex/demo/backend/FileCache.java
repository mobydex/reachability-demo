package org.aksw.mobydex.demo.backend;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.aksw.commons.io.util.PathUtils;
import org.apache.jena.atlas.io.IOX;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.core.DatasetGraph;

public class FileCache {
    private Cache<List<String>, Object> cache;
    private Path cacheBasePath;

    public FileCache(Path cacheBasePath) {
        super();
        this.cacheBasePath = cacheBasePath;
        try {
            Files.createDirectories(cacheBasePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.cache = Caffeine.newBuilder().maximumSize(100).build();
    }

    public Path getCacheBasePath() {
        return cacheBasePath;
    }

    public Cache<List<String>, Object> getCache() {
        return cache;
    }

    public InputStream open(List<String> key, Callable<InputStream> sourceSupplier) throws IOException {
        Path path = PathUtils.resolve(cacheBasePath, key);
        System.err.println("Processing: " + path);
        if (!Files.exists(path)) {
            Path tmpFile;
            try {
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }
                tmpFile = Files.createTempFile("inputstream-", ".dat");
                try (InputStream in = sourceSupplier.call()) {
                    IOX.safeWriteOrCopy(path, tmpFile, out -> in.transferTo(out));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        InputStream result = Files.newInputStream(path);
        return result;
    }

    /** Generic cached model loading. */
    public Model loadModel(List<String> key, Callable<Model> creator) {
        Model result = (Model)cache.get(key, k -> {
            Model r;
            Path path = PathUtils.resolve(cacheBasePath, key);
            System.err.println("Processing: " + path);
            if (Files.exists(path)) {
                r = RDFDataMgr.loadModel(path.toString());
            } else {
                Path tmpFile;
                try {
                    if (path.getParent() != null) {
                        Files.createDirectories(path.getParent());
                    }
                    tmpFile = Files.createTempFile("model-", ".ttl");
                    r = creator.call();
                    IOX.safeWriteOrCopy(path, tmpFile, out -> RDFDataMgr.write(out, r, RDFFormat.TURTLE_BLOCKS));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return FileCaches.readOnly(r);
        });
        return result;
    }


    public Resource loadResourceOld(List<String> key, Callable<Resource> creator) {
        Resource result = (Resource)cache.get(key, k -> {
            Resource tmp;
            Path path = PathUtils.resolve(cacheBasePath, key);
            System.err.println("Processing: " + path);
            if (!Files.exists(path)) {
                Path tmpFile;
                try {
                    if (path.getParent() != null) {
                        Files.createDirectories(path.getParent());
                    }
                    tmpFile = Files.createTempFile("resource-", ".trig");
                    tmp = creator.call();
                    DatasetGraph dsg = FileCaches.resourceToDataset(tmp);
                    IOX.safeWriteOrCopy(path, tmpFile, out -> RDFDataMgr.write(out, dsg, RDFFormat.TRIG_BLOCKS));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                DatasetGraph dsg = RDFDataMgr.loadDatasetGraph(path.toString(), Lang.TRIG);
                tmp = FileCaches.datasetToResource(dsg);
            }
            Resource readOnly = FileCaches.readOnly(tmp.getModel()).asRDFNode(tmp.asNode()).asResource();
            return readOnly;
        });
        return result;
    }
}
