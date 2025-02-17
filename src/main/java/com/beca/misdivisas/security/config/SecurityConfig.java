package com.beca.misdivisas.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.beca.misdivisas.interfaces.ILogRepo;

@EnableWebSecurity
@Configuration
@EnableAutoConfiguration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("${ldap.oficinas.dominio}")
	private String ldapOficinasDominio;

	@Value("${ldap.oficinas.ip}")
	private String ldapOficinasIP;
	
	@Value("${ldap.oficinas.puerto}")
	private String ldapOficinasPuerto;
	
	@Value("${ldap.torre.dominio}")
	private String ldapTorreDominio;

	@Value("${ldap.torre.ip}")
	private String ldapTorreIP;
	
	@Value("${ldap.torre.puerto}")
	private String ldapTorrePuerto;
	
	@Autowired
	private UserDetailsService userDetailsService;	
	
	//Cambios JF
	@Autowired
    private CustomWebAuthenticationDetailsSource authenticationDetailsSource;
	
	
	@Autowired
	private BCryptPasswordEncoder bCrypt;
	
	@Autowired
	private ILogRepo logRepo;
	
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
		return bCryptPasswordEncoder;

	}
	

	//Cambios JF
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
			//Clientes externos BD
			auth.authenticationProvider(activeDAOAuthenticationProvider());
			//Clientes internos LDAP Torre
			AuthenticationProvider adTorre = activeDirectoryLdapAuthenticationProviderTorre(ldapTorreDominio, ldapTorreIP, ldapTorrePuerto);
			auth.authenticationProvider(adTorre);
			//Clientes internos LDAP Oficinas
			AuthenticationProvider adOficinas = activeDirectoryLdapAuthenticationProviderOficinas(ldapOficinasDominio, ldapOficinasIP, ldapOficinasPuerto);
			auth.authenticationProvider(adOficinas);
	}

	//Cambios JF
	@Bean
	public AuthenticationProvider activeDirectoryLdapAuthenticationProviderTorre(String domain, String ip, String puerto) {
		CustomActiveDirectoryLdapAuthenticationProvider provider = new CustomActiveDirectoryLdapAuthenticationProvider(
				domain, ip, puerto);
		provider.setConvertSubErrorCodesToExceptions(true);
		provider.setUseAuthenticationRequestCredentials(true);
		//provider.setRoleDao(roleDao);
		return provider;
	}
	
	@Bean
	public AuthenticationProvider activeDirectoryLdapAuthenticationProviderOficinas(String domain, String ip, String puerto) {
		CustomActiveDirectoryLdapAuthenticationProvider provider = new CustomActiveDirectoryLdapAuthenticationProvider(
				domain, ip, puerto);
		provider.setConvertSubErrorCodesToExceptions(true);
		provider.setUseAuthenticationRequestCredentials(true);
		//provider.setRoleDao(roleDao);
		return provider;
	}
	
	@Bean
	public DaoAuthenticationProvider activeDAOAuthenticationProvider() {
	    CustomDAOAuthenticationProvider authProvider = new CustomDAOAuthenticationProvider();
		//DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
	    authProvider.setUserDetailsService(userDetailsService);
	    authProvider.setPasswordEncoder(bCrypt);
	    return authProvider;
	}
	
	@Override
    protected void configure(HttpSecurity http) throws Exception {

		 http
		 .csrf().disable()		 
		 .authorizeRequests()		 
		 .antMatchers("/css/**", "/ol/**",
		 "/img/**",
		 "/js/**",
		 "/scss/**",
		 "/tmp/**",
		 "/vendor/**").permitAll()		 
		 .antMatchers("/login*").permitAll()		 
		 .anyRequest().authenticated()		 
         .and()
         .formLogin()
         .loginPage("/login") //the custom login page
         .loginProcessingUrl("/login") //the url to submit the username and password to
         .usernameParameter("username")
         .passwordParameter("password")
         .successHandler(new CustomAuthenticationSuccessHandler(logRepo))
       //Cambios JF agregar esta linea
         .authenticationDetailsSource(authenticationDetailsSource) 
		 //.defaultSuccessUrl("/main", true)  //the landing page after a successful login
         .failureUrl("/login?error") //the landing page after an unsuccessful login
         //.failureHandler(authenticationFailureHandler())         
         .and()		 
		 .logout().logoutRequestMatcher(new AntPathRequestMatcher("/logout")).clearAuthentication(true)
		 .addLogoutHandler(new CustomLogoutHandler(logRepo))
		 .logoutSuccessUrl("/login?logout").deleteCookies("JSESSIONID")
		 .invalidateHttpSession(true);
    }
}