package org.aksw.mobydex.demo;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.aura.Aura;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@PWA(name = "MobyDex Demonstrator", shortName = "MobyDex Demo",
    description = "This is a demonstrator of components for semantic data.")
@StyleSheet(Aura.STYLESHEET) // Specifically loads Lumo base styles
// @StyleSheet("context://themes/my-theme/styles.css") // Loads your custom styles
@Push
public class MainAppVaadinMobydexDemo
    implements AppShellConfigurator
{
    private static final long serialVersionUID = 1L;

    public static void main(String[] args) {
        SpringApplication.run(MainAppVaadinMobydexDemo.class, args);
//        ConfigurableApplicationContext cxt = new SpringApplicationBuilder()
//                .bannerMode(Mode.OFF)
//                .sources(MainAppComponentDemoVaadin.class)
//                .run(args);
    }
}
