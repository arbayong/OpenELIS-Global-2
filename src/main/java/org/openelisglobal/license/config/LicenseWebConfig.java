package org.openelisglobal.license.config;

import java.util.EnumSet;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.filter.DelegatingFilterProxy;

public class LicenseWebConfig implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) {
        FilterRegistration.Dynamic reg = servletContext.addFilter("licenseFilter",
                new DelegatingFilterProxy("licenseFilter"));
        reg.setAsyncSupported(true);
        reg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
    }
}
