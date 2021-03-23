package com.dulithadabare.dosomething;

import com.dulithadabare.dosomething.model.BasicResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.sql.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

@Configuration
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter
{
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri;
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
                .authorizeRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .cors().and()
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
        // @formatter:on
    }

    CorsConfigurationSource corsConfigurationSource()
    {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins( Arrays.asList( "*" ) );
        configuration.setAllowedMethods( Arrays.asList( "HEAD", "GET", "POST", "PUT", "DELETE", "PATCH" ) );
        configuration.setAllowedHeaders( Arrays.asList( "Authorization", "Cache-Control", "Content-Type" ) );
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration( "/**", configuration );
        return source;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        MappedJwtClaimSetConverter converter = MappedJwtClaimSetConverter.withDefaults(Collections.singletonMap("sub", this::getUserIdByFirebaseUid));
//        MappedJwtClaimSetConverter converter = MappedJwtClaimSetConverter.withDefaults(Collections.singletonMap("custom", custom -> "test_user_id_value"));
        jwtDecoder.setClaimSetConverter(converter);

        return jwtDecoder;
    }

    Integer getUserIdByFirebaseUid(Object uid )
    {
        Integer userId = -1;
        try( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {
            String checkNew = "SELECT u.id FROM user u WHERE  u.firebase_uid = ?";

            try ( PreparedStatement ps = conn.prepareStatement( checkNew ) )
            {

                ps.setFetchSize( 1000 );

                ps.setString( 1, (String) uid );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        userId = rs.getInt( 1 );
                    }

                } catch ( SQLException e )
                {
                    e.printStackTrace();
                    return null;
                }
            } catch ( SQLException e )
            {
                e.printStackTrace();
                return null;
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return null;
        }

        return userId;
    }
}