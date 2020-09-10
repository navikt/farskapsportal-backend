package no.nav.farskapsportal.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import lombok.val;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.lang.Nullable;

public class YamlPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException {
        val propertiesFromYaml = loadYamlIntoProperties(resource);
        val sourceName = name != null ? name : resource.getResource().getFilename();
        return new PropertiesPropertySource(sourceName, propertiesFromYaml);
    }

    private Properties loadYamlIntoProperties(EncodedResource resource) throws FileNotFoundException {
        try {
            val factory = new YamlPropertiesFactoryBean();
            factory.setResources(resource.getResource());
            factory.afterPropertiesSet();
            return factory.getObject();
        } catch (IllegalStateException e) {
            // for ignoreResourceNotFound
            val cause = e.getCause();
            if (cause instanceof FileNotFoundException) {
                throw (FileNotFoundException) e.getCause();
            }
            throw e;
        }
    }
}
