package org.openelisglobal.billing.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Date;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Maps an OpenELIS local test code to the NHIA GDRG tariff code. The claim XML
 * must carry the GDRG code as TreatmentCode, never the local code, so every
 * billable test needs a row here before its claims can validate. Rows are
 * effective-dated like the CLAIM-it tariff schedules.
 */
@Entity
@Table(name = "billing_gdrg_mapping")
public class GdrgMapping extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "billing_gdrg_mapping_generator")
    @SequenceGenerator(name = "billing_gdrg_mapping_generator", sequenceName = "billing_gdrg_mapping_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "local_code", length = 50, nullable = false)
    private String localCode;

    @Column(name = "gdrg_code", length = 60, nullable = false)
    private String gdrgCode;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "effective_from")
    private Date effectiveFrom;

    @Column(name = "effective_to")
    private Date effectiveTo;

    /** "Y"/"N", matching the other billing flags. */
    @Column(name = "is_active", length = 1)
    private String isActive;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getLocalCode() {
        return localCode;
    }

    public void setLocalCode(String localCode) {
        this.localCode = localCode;
    }

    public String getGdrgCode() {
        return gdrgCode;
    }

    public void setGdrgCode(String gdrgCode) {
        this.gdrgCode = gdrgCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
