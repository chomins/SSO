package com.example.SP;

import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class SpApplication {
	private final static Logger LOGGER = LoggerFactory.getLogger(SpApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(SpApplication.class, args);
	}

	@Configuration
	public static class MvcConfig implements WebMvcConfigurer {
		public void addViewControllers(ViewControllerRegistry registry) {
			registry.addViewController("/").setViewName("index");
			registry.addViewController("/user").setViewName("user");
		}
	}

	@Component
	public static class SamlBootstrap implements BeanFactoryPostProcessor {

		private static final Logger LOGGER = LoggerFactory.getLogger(SamlBootstrap.class);

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			try {
				LOGGER.info("Initialize open saml...");
				DefaultBootstrap.bootstrap();
			} catch (ConfigurationException e) {
				throw new FatalBeanException("Error invoking OpenSAML bootstrap", e);
			}
		}
	}

	@Controller
	public static class SamlController {
		@GetMapping("/user")
		public String user(Model model, Authentication authentication) {
			model.addAttribute("samlUser", authentication.getDetails());
			return "user";
		}
	}

}
