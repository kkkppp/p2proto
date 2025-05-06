package org.p2proto.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableWebMvc
@EnableTransactionManagement
@ComponentScan(basePackages = "org.p2proto.config")
public class SecurityConfig implements WebMvcConfigurer {

    @Autowired
    private KeycloakProperties keycloakProperties;

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        PropertySourcesPlaceholderConfigurer propertyConfigurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        propertyConfigurer.setProperties(yaml.getObject());
        return propertyConfigurer;
    }
    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository
                        .withHttpOnlyFalse())
                )
            .authorizeRequests()
                .antMatchers("/login", "/resources/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .oauth2Login() // Enables OAuth2 login with Keycloak
                .clientRegistrationRepository(clientRegistrationRepository)
                .authorizedClientService(authorizedClientService)
            .and()
                .logout()
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    if (authentication instanceof OAuth2AuthenticationToken) {
                        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                        OAuth2AuthorizedClient authorizedClient = authorizedClientService
                                .loadAuthorizedClient(
                                        oauthToken.getAuthorizedClientRegistrationId(),
                                        oauthToken.getName()
                                );

                        if (authorizedClient != null) {
                            // Get the ID token (not access token)
                            OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
                            String idToken = oidcUser.getIdToken().getTokenValue();

                            String redirectUri = keycloakProperties.getPostLogoutRedirectUri();
                            String logoutUrl = keycloakProperties.getKeycloakLogoutUri() +
                                    "?post_logout_redirect_uri=" + redirectUri +
                                    "&id_token_hint=" + idToken;

                            response.sendRedirect(logoutUrl);
                            return;
                        }
                    }
                    response.sendRedirect("/"); // Fallback if no token is found
                });

        return http.build();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map all /resources/** URLs to the /resources/ directory in webapp
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
                //.setCachePeriod(3600) // Optional: cache for 1 hour
                //.resourceChain(true);
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
                //.setCachePeriod(3600) // Optional: cache for 1 hour
                //.resourceChain(true);
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        // "messages" maps to messages.properties on the classpath
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

}
