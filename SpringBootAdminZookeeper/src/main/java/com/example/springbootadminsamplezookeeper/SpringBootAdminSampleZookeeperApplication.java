package com.example.springbootadminsamplezookeeper;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAdminServer
public class SpringBootAdminSampleZookeeperApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootAdminSampleZookeeperApplication.class, args);
    }


    @Profile("insecure")
    @Configuration(proxyBeanMethods = false)
    public static class SecurityPermitAllConfig extends WebSecurityConfigurerAdapter {

        private final AdminServerProperties adminServer;

        public SecurityPermitAllConfig(AdminServerProperties adminServer) {
            this.adminServer = adminServer;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests((authorizeRequests) -> authorizeRequests.anyRequest().permitAll())
                    .csrf((csrf) -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                            .ignoringRequestMatchers(
                                    new AntPathRequestMatcher(this.adminServer.path("/instances"),
                                            HttpMethod.POST.toString()),
                                    new AntPathRequestMatcher(this.adminServer.path("/instances/*"),
                                            HttpMethod.DELETE.toString()),
                                    new AntPathRequestMatcher(this.adminServer.path("/actuator/**"))));
        }

    }

    @Profile("secure")
    @Configuration(proxyBeanMethods = false)
    public static class SecuritySecureConfig extends WebSecurityConfigurerAdapter {

        private final AdminServerProperties adminServer;

        private final SecurityProperties security;

        public SecuritySecureConfig(AdminServerProperties adminServer,SecurityProperties security) {
            this.adminServer = adminServer;
            this.security = security;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
            successHandler.setTargetUrlParameter("redirectTo");
            successHandler.setDefaultTargetUrl(this.adminServer.path("/"));

            http.authorizeRequests((authorizeRequests) -> authorizeRequests
                    .antMatchers(this.adminServer.path("/assets/**")).permitAll()
                    .antMatchers(this.adminServer.path("/login")).permitAll().anyRequest().authenticated())
                    .formLogin((formLogin) -> formLogin.loginPage(this.adminServer.path("/login"))
                            .successHandler(successHandler))
                    .logout((logout) -> logout.logoutUrl(this.adminServer.path("/logout")))
                    .httpBasic(Customizer.withDefaults())
                    .csrf((csrf) -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                            .ignoringRequestMatchers(
                                    new AntPathRequestMatcher(this.adminServer.path("/instances"),
                                            HttpMethod.POST.toString()),
                                    new AntPathRequestMatcher(this.adminServer.path("/instances/*"),
                                            HttpMethod.DELETE.toString()),
                                    new AntPathRequestMatcher(this.adminServer.path("/actuator/**"))));
        }

        /**
         * 记住我功能配置，当点击记住我时候，必须有以下配置，否则无法登录成功。
         * @param auth
         * @throws Exception
         */
        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication().withUser(security.getUser().getName())
                    .password("{noop}" + security.getUser().getPassword()).roles("USER");
        }

    }

}
