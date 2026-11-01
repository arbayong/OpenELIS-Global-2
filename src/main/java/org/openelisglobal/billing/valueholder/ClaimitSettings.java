package org.openelisglobal.billing.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * The reusable CLAIM-it integration configuration for this installation:
 * where the local instance lives, the facility credentials, and the NHIA
 * provider identifiers. CLAIM-it maintains the credential itself and it is
 * renewed there, so this row is configuration, not a per-claim record.
 */
@Entity
@Table(name = "claimit_settings")
public class ClaimitSettings extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "claimit_settings_generator")
    @SequenceGenerator(name = "claimit_settings_generator", sequenceName = "claimit_settings_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    /** Local CLAIM-it API, e.g. http://localhost:31719/json-api */
    @Column(name = "base_url", length = 200, nullable = false)
    private String baseUrl;

    @Column(name = "username", length = 120)
    private String username;

    /** Encrypted with the gateway master key; never returned by the API. */
    @Column(name = "password_encrypted", length = 500)
    private String passwordEncrypted;

    @Column(name = "provider_id", length = 60)
    private String providerId;

    @Column(name = "provider_name", length = 200)
    private String providerName;

    @Column(name = "facility_type", length = 120)
    private String facilityType;

    @Column(name = "region", length = 120)
    private String region;

    /** Tariff/provider tier the facility bills under. */
    @Column(name = "provider_tier", length = 30)
    private String providerTier;

    @Column(name = "last_updated")
    private Timestamp lastUpdated;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordEncrypted() {
        return passwordEncrypted;
    }

    public void setPasswordEncrypted(String passwordEncrypted) {
        this.passwordEncrypted = passwordEncrypted;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(String facilityType) {
        this.facilityType = facilityType;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getProviderTier() {
        return providerTier;
    }

    public void setProviderTier(String providerTier) {
        this.providerTier = providerTier;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
