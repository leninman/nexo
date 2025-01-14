package com.beca.misdivisas.security.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import com.beca.misdivisas.security.interceptor.UserContextInterceptor;

@Configuration
public class Internationalization implements WebMvcConfigurer  {
	
   @Autowired	
   private UserContextInterceptor userContextInterceptor;
   
   @Bean
   public LocaleResolver localeResolver() {
     SessionLocaleResolver localeResolver = new SessionLocaleResolver();
     localeResolver.setDefaultLocale(Locale.getDefault());
     return localeResolver;
   }

   @Bean
   public LocaleChangeInterceptor localeChangeInterceptor() {
     LocaleChangeInterceptor localeInterceptor = new LocaleChangeInterceptor();
     localeInterceptor.setIgnoreInvalidLocale(true);
     localeInterceptor.setParamName("idioma");
     return localeInterceptor;
   }

   @Override
   public void addInterceptors(InterceptorRegistry registry) {
     registry.addInterceptor(localeChangeInterceptor());
     registry.addInterceptor(userContextInterceptor).excludePathPatterns("/vendor/**", "/css/**", "/js/**", "/img/**", "/images/**");
   }
}
