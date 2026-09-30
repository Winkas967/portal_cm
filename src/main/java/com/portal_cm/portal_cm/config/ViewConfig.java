package com.portal_cm.portal_cm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Liga cada URL de página ao seu template em src/main/resources/templates.
 * As páginas não recebem dados do servidor: o JavaScript de cada uma busca tudo na API.
 */
@Configuration
public class ViewConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/fichas");
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/fichas").setViewName("fichas");
        registry.addViewController("/fichas/nova").setViewName("ficha-nova");
        registry.addViewController("/fichas/detalhe").setViewName("ficha-detalhe");
        registry.addViewController("/admin").setViewName("admin");
    }
}
