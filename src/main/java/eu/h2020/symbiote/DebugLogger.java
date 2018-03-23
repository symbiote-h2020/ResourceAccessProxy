package eu.h2020.symbiote;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

@Configuration
public class DebugLogger {
	private static final Logger log = LoggerFactory.getLogger(DebugLogger.class);

	@Autowired
	private AbstractEnvironment environment;
	
	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx, AbstractEnvironment environment) {
		return args -> {
			printProperties();
			printBeans(ctx);
		};
	}

	public void printProperties() {

		log.debug("**** APPLICATION PROPERTIES SOURCES ****");

		Set<String> properties = new TreeSet<>();
		for (PropertiesPropertySource p : findPropertiesPropertySources()) {
			log.debug(p.toString());
			properties.addAll(Arrays.asList(p.getPropertyNames()));
		}

		log.debug("**** APPLICATION PROPERTIES VALUES ****");
		print(properties);

	}

	private List<PropertiesPropertySource> findPropertiesPropertySources() {
		List<PropertiesPropertySource> propertiesPropertySources = new LinkedList<>();
		for (PropertySource<?> propertySource : environment.getPropertySources()) {
			if (propertySource instanceof PropertiesPropertySource) {
				log.debug("Used property source: {}", propertySource.getName());
				propertiesPropertySources.add((PropertiesPropertySource) propertySource);
			} else {
				log.debug("Dismissed property source: {}", propertySource.getName());
			}
		}
		return propertiesPropertySources;
	}

	private void print(Set<String> properties) {
		MutablePropertySources propertySources = environment.getPropertySources();
		for (String propertyName : properties) {
			for(PropertySource<?> propertySource : propertySources) {
				if(propertySource.containsProperty(propertyName)) {
					if(propertySource instanceof CompositePropertySource) {
						CompositePropertySource compositeSoruce = (CompositePropertySource) propertySource;
						log.debug("{}={}\ts:{}", propertyName, environment.getProperty(propertyName), 
								composeSourceName(compositeSoruce, propertyName));
					} else {
						log.debug("{}={}\ts:{}", propertyName, environment.getProperty(propertyName), propertySource.getName());
					}
					break;
				}
			}
		}
	}

	private String composeSourceName(CompositePropertySource compositeSoruce, String propertyName) {
		for(PropertySource<?> source: compositeSoruce.getPropertySources()) {
			if(source.containsProperty(propertyName)) {
				if(source instanceof CompositePropertySource) {
					return compositeSoruce.getName() + " -> " + composeSourceName((CompositePropertySource) source, propertyName);
				} else {
					return compositeSoruce.getName() + " -> " + source.getName();
				}
			}
		}
		return compositeSoruce.getName();
	}

	private void printBeans(ApplicationContext ctx) {
		log.debug("**** SPRING BEANS ****");

		String[] beanNames = ctx.getBeanDefinitionNames();
		Arrays.sort(beanNames);
		for (String beanName : beanNames) {
			Object bean = ctx.getBean(beanName);
			if(bean != null)
				log.debug("{}: {}", beanName, bean.getClass().getName());
			else
				log.debug("{}: {}", beanName, bean);
		}
	}
}