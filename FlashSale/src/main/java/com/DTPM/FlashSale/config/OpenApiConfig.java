package com.DTPM.FlashSale.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Flash Sale Ticketing API", version = "1.0", description = "Base BE e-ticketing — JWT + Redis flash sale hold 10 phút"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

        @Bean
        public OpenAPI openAPI() {
                return new OpenAPI()
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                                .components(new Components().addSecuritySchemes("bearerAuth",
                                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                                                .name("bearerAuth")
                                                                .type(Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")))
                                .tags(List.of(
                                                new Tag().name("Auth")
                                                                .description("Đăng ký, đăng nhập, token, đổi mật khẩu"),
                                                new Tag().name("Events")
                                                                .description("Sự kiện công khai + Organizer CRUD"),
                                                new Tag().name("TicketTypes")
                                                                .description("Cấu hình loại vé + availability"),
                                                new Tag().name("Orders")
                                                                .description("Đặt vé, hủy, thanh toán mock, lịch sử"),
                                                new Tag().name("Admin")
                                                                .description("Ban/unban user, thống kê hệ thống")));
        }

        @Bean
        public GroupedOpenApi authGroup() {
                return GroupedOpenApi.builder()
                                .group("Auth")
                                .pathsToMatch("/api/auth/**")
                                .build();
        }

        @Bean
        public GroupedOpenApi eventsGroup() {
                return GroupedOpenApi.builder()
                                .group("Events")
                                .pathsToMatch("/api/events/**", "/api/organizer/events/**")
                                .build();
        }

        @Bean
        public GroupedOpenApi ticketTypesGroup() {
                return GroupedOpenApi.builder()
                                .group("TicketTypes")
                                .pathsToMatch("/api/organizer/events/*/tickets/**", "/api/organizer/events/tickets/**", "/api/events/*/tickets/**")
                                .build();
        }

        @Bean
        public GroupedOpenApi ordersGroup() {
                return GroupedOpenApi.builder()
                                .group("Orders")
                                .pathsToMatch("/api/orders/**")
                                .build();
        }

        @Bean
        public GroupedOpenApi sessionsGroup() {
                return GroupedOpenApi.builder()
                                .group("Sessions")
                                .pathsToMatch("/api/sessions/**")
                                .build();
        }

        @Bean
        public GroupedOpenApi ticketsGroup() {
                return GroupedOpenApi.builder()
                                .group("Tickets")
                                .pathsToMatch("/api/tickets/**")
                                .build();
        }

        @Bean
        public GroupedOpenApi adminGroup() {
                return GroupedOpenApi.builder()
                                .group("Admin")
                                .pathsToMatch("/api/admin/**")
                                .build();
        }
}