package org.aksw.mobydex.demo;

import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.TableFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserWorkspace {

    private Long projectId;
    private Long computationId;
    // private Set<String> poiTypes = new HashSet<>();
    private Table poiTypes;
    private Table geometries;


    // getters + setters + clear() method etc.
    public void clear() {
        projectId = null;
        computationId = null;
        //poiTypes.clear();
        poiTypes = TableFactory.create();
        geometries = TableFactory.create();
    }
}
