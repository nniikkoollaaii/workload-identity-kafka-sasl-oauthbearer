package io.github.nniikkoollaaii.schemaregistryauthn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SchemaRegistryAuthnApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchemaRegistryAuthnApplication.class, args);
	}

	@Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("schema_registry", r -> r.path("/**")
                        .uri("http://schema-registry:8081"))
                .build();
    }

}
