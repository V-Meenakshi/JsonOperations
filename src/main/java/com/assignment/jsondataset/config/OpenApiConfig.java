package com.assignment.jsondataset.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadata shown at the top of Swagger UI (http://localhost:8080/swagger-ui.html). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI datasetServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("JSON Dataset Service API")
                .version("1.0.0")
                .description("""
                        Store JSON records in named datasets and query them with group-by and sort-by operators.

                        Errors are returned as RFC 9457 `application/problem+json`."""));
    }
}
