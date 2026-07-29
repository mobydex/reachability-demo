package org.aksw.mobydex.demo.domain;

import java.math.BigDecimal;

import org.aksw.jenax.annotation.reprogen.IriNs;
import org.aksw.jenax.annotation.reprogen.Namespace;
import org.apache.jena.rdf.model.Resource;

@Namespace(prefix = "eg", value = "http://www.example.org/")
public interface Computation
    extends Resource
{
    @IriNs("eg")
    Integer getComputationId();

    @IriNs("eg")
    GridCell getOriginCell();

    @IriNs("eg")
    GridCell getDestinationCell();

    @IriNs("eg")
    BigDecimal getDurationMax();

    @IriNs("eg")
    BigDecimal getDurationMin();

    @IriNs("eg")
    BigDecimal getDurationAvg();

    @IriNs("eg")
    BigDecimal getDurationMedian();
}
