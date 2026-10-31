package org.openelisglobal.billing.service;

import java.util.List;
import org.openelisglobal.billing.dao.BillingConfigDAO;
import org.openelisglobal.billing.valueholder.BillingConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the billing flags from the billing_config table (plan §9).
 *
 * <p>
 * Keys are the flat names configured per deployment, optionally with a suffix
 * for the per-driver gateway flags ({@code gateway.paystack.enabled}). Every
 * read fails closed to the supplied default, so a missing or unparseable flag
 * never blocks billing: a site that has not configured anything gets the
 * documented defaults rather than an error.
 */
@Service
@Transactional
public class BillingConfigService {

    public static final String KEY_MODULE_ENABLED = "billing.module.enabled";
    public static final String KEY_TARIFF_LOOKUP = "billing.tariff.lookup";
    public static final String KEY_NHIS_ENABLED = "nhis.enabled";
    public static final String KEY_NHIS_XML_VERSION = "nhis.xml.version";
    public static final String KEY_GATEWAY_PREFIX = "gateway.";
    public static final String KEY_GATEWAY_SUFFIX = ".enabled";

    /** Defaults matching the plan: the module works out of the box, and the
     *  Ghana-specific surfaces are the ones a site opts into. */
    public static final boolean DEFAULT_MODULE_ENABLED = true;
    public static final boolean DEFAULT_TARIFF_LOOKUP = true;
    public static final boolean DEFAULT_NHIS_ENABLED = true;
    public static final String DEFAULT_NHIS_XML_VERSION = "8.6";

    @Autowired
    private BillingConfigDAO billingConfigDAO;

    /** The stored value for a key, or null when unset or unreadable. */
    @Transactional(readOnly = true)
    public String getValue(String key) {
        try {
            List<BillingConfig> configs = billingConfigDAO.getByConfigKey(key);
            if (configs == null || configs.isEmpty()) {
                return null;
            }
            String value = configs.get(0).getConfigValue();
            return value == null || value.trim().isEmpty() ? null : value.trim();
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(String key, boolean defaultValue) {
        String value = getValue(key);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "Y".equalsIgnoreCase(value) || "1".equals(value);
    }

    @Transactional(readOnly = true)
    public boolean isModuleEnabled() {
        return isEnabled(KEY_MODULE_ENABLED, DEFAULT_MODULE_ENABLED);
    }

    @Transactional(readOnly = true)
    public boolean isTariffLookupEnabled() {
        return isEnabled(KEY_TARIFF_LOOKUP, DEFAULT_TARIFF_LOOKUP);
    }

    @Transactional(readOnly = true)
    public boolean isNhisEnabled() {
        return isEnabled(KEY_NHIS_ENABLED, DEFAULT_NHIS_ENABLED);
    }

    /** The configured NHIA schema version, e.g. "8.6". */
    @Transactional(readOnly = true)
    public String getNhisXmlVersion() {
        String value = getValue(KEY_NHIS_XML_VERSION);
        return value == null ? DEFAULT_NHIS_XML_VERSION : value;
    }

    /**
     * A driver is enabled when its flag is true, or when no flag is set and the
     * module-level default applies. A key of {@code gateway.paystack.enabled}
     * reads exactly that key.
     */
    @Transactional(readOnly = true)
    public boolean isGatewayEnabled(String driver) {
        if (driver == null || driver.trim().isEmpty()) {
            return false;
        }
        String key = KEY_GATEWAY_PREFIX + driver.trim().toLowerCase() + KEY_GATEWAY_SUFFIX;
        return isEnabled(key, DEFAULT_MODULE_ENABLED);
    }
}
