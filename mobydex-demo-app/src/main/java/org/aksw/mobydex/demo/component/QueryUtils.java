package org.aksw.mobydex.demo.component;

import java.util.List;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpMinus;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementMinus;
import org.apache.jena.sparql.syntax.ElementSubQuery;

public class QueryUtils {
    public static Query join(Query query, Table table) {
        Query clone = query.cloneQuery();
        List<Binding> bindings = Iter.toList(table.rows());
        clone.setValuesDataBlock(table.getVars(), bindings);
        return clone;
    }

    public static Query joinViaOp(Query query, Table table) {
        // org.aksw.jenax.arq.util.syntax.QueryUtils.rest
        Op left = Algebra.compile(query);
        OpTable right = OpTable.create(table);
        Op join = OpJoin.create(left, right);
        Query q = OpAsQuery.asQuery(join);
        return q;
    }

    public static Query minusViaOp(Query query, Table table) {
        // org.aksw.jenax.arq.util.syntax.QueryUtils.rest
        Op left = Algebra.compile(query);
        OpTable right = OpTable.create(table);
        Op minus = OpMinus.create(left, right);
        Query q = OpAsQuery.asQuery(minus);
        return q;
    }

    public static Query minus(Query query, Table table) {
        List<Binding> bindings = Iter.toList(table.rows());

        ElementGroup elt = new ElementGroup();
        elt.addElement(new ElementSubQuery(query));
        elt.addElement(new ElementMinus(new ElementData(table.getVars(), bindings)));

        Query result = new Query();
        result.setQuerySelectType();
        result.setQueryResultStar(true);
        result.setQueryPattern(elt);
        return result;
    }


    public static void main(String[] args) {
        Query query = QueryFactory.create("SELECT ?s ?o { ?s <http://www.example.org> ?o }");
        Table table = SSE.parseTable("""
            (table
                (row (?s eg:foo) (?o 1))
                (row (?s eg:bar) (?o 2))
            )
            """);

        System.out.println(minus(query, table));
        System.out.println(join(query, table));
        System.out.println(joinViaOp(query, table));
    }
}
