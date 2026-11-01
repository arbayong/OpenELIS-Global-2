package org.openelisglobal.billing.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Date;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * A versioned NHIA G-DRG tariff schedule. CLAIM-it publishes tariff schedules
 * per provider tier; each schedule carries a version string and an effective
 * window. GDRG mappings reference a schedule through version_id.
 */
@Entity
@Table(name = "billing_tariff_version")
public class TariffVersion extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "billing_tariff_version_generator")
    @SequenceGenerator(name = "billing_tariff_version_generator", sequenceName = "billing_tariff_version_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "version", length = 30, nullable = false)
    private String version;

    @Column(name = "name", length = 120)
    private String name;

    @Column(name = "provider_tier", length = 30)
    private String providerTier;

    @Column(name = "effective_from")
    private Date effectiveFrom;

    @Column(name = "effective_to")
    private Date effectiveTo;

    /** "Y"/"N", matching the other billing flags. */
    @Column(name = "is_active", length = 1)
    private String isActive;

    @Column(name = "source", length = 255)
    private String source;

    @Column(name = "imported_at")
    private Timestamp importedAt;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProviderTier() {
        return providerTier;
    }

    public void setProviderTier(String providerTier) {
        this.providerTier = providerTier;
    }

    public Date getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Date effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Date getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Date effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Timestamp getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(Timestamp importedAt) {
        this.importedAt = importedAt;
    }
}
