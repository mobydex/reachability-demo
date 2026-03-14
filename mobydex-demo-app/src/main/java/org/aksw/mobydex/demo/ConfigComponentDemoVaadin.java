package org.aksw.mobydex.demo;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigComponentDemoVaadin {
//    @Bean
//    public LabelServiceSwitchable<Node, String> labelService() {
//        RDFDataSource base = () -> RDFConnection.connect("http://localhost:8642/sparql");
//        RDFDataSource dataSource = RDFDataSources.decorate(base, RDFDataSourceWithBnodeRewrite.asTransform());
//
//        QueryExecutionFactoryQuery qef = QueryExecutionFactories.of(dataSource); // QueryExecutionFactories.of(dataSource);
//        Property labelProperty = RDFS.label;// DCTerms.description;
//
//        LookupService<Node, String> ls1 = LabelUtils.getLabelLookupService(qef, labelProperty, DefaultPrefixes.get(), 30);
//        LookupService<Node, String> ls2 = keys -> Flowable.fromIterable(keys).map(k -> Map.entry(k, Objects.toString(k)));
//
//        VaadinLabelMgr<Node, String> labelMgr = new VaadinLabelMgr<>(ls1);
//
//        LabelServiceSwitchable<Node, String> result = new LabelServiceSwitchableImpl<>(labelMgr);
//        result.getLookupServices().addAll(Arrays.asList(ls1, ls2));
//
//        return result;
//    }

}
