package org.openelisglobal.license.filter;

import java.io.IOException;
import java.util.Map;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openelisglobal.license.model.LicenseStatus;
import org.openelisglobal.license.service.FingerprintService;
import org.openelisglobal.license.service.LicenseService;
import org.openelisglobal.license.service.TrialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LicenseFilter implements Filter {

    @Autowired
    private FingerprintService fingerprintService;

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private TrialService trialService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();
        if (isExempt(path)) {
            chain.doFilter(request, response);
            return;
        }
        String fp = fingerprintService.computeFingerprint();
        if (licenseService.validate() == LicenseStatus.VALID) {
            chain.doFilter(request, response);
            return;
        }
        if (trialService.isTrialValid(fp)) {
            chain.doFilter(request, response);
            return;
        }
        httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        httpResponse.setContentType("application/json");
        httpResponse.setCharacterEncoding("UTF-8");
        Map<String, Object> body = Map.of("locked", true, "reason", "license required", "fingerprint", fp);
        new ObjectMapper().writeValue(httpResponse.getWriter(), body);
    }

    private boolean isExempt(String path) {
        if (path == null) {
            return true;
        }
        if (path.startsWith("/rest/license")) {
            return true;
        }
        if (path.startsWith("/license")) {
            return true;
        }
        if (path.endsWith(".css")) {
            return true;
        }
        if (path.endsWith(".js")) {
            return true;
        }
        return false;
    }
}
