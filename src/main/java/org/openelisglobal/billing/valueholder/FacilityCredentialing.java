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

@Entity
@Table(name = "facility_credentialing")
public class FacilityCredentialing extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "facility_credentialing_generator")
    @SequenceGenerator(name = "facility_credentialing_generator", sequenceName = "facility_credentialing_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "organization_id")
    private Integer organizationId;

    @Column(name = "technical_head_name", length = 200)
    private String technicalHeadName;

    @Column(name = "technical_head_license_number", length = 60)
    private String technicalHeadLicenseNumber;

    @Column(name = "prescribing_level", length = 20)
    private String prescribingLevel;

    @Column(name = "specialities", length = 2000)
    private String specialities;

    @Column(name = "accreditation_number", length = 60)
    private String accreditationNumber;

    /** e-Claim authorization number, sent in the batch ProviderInformation. */
    @Column(name = "eclaim_authorization_number", length = 60)
    private String eclaimAuthorizationNumber;

    @Column(name = "level_of_care", length = 60)
    private String levelOfCare;

    @Column(name = "valid_from")
    private Date validFrom;

    @Column(name = "valid_to")
    private Date validTo;

    @Column(name = "is_active", length = 1)
    private String isActive;

    @Column(name = "source_file", length = 500)
    private String sourceFile;

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

    public Integer getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Integer organizationId) {
        this.organizationId = organizationId;
    }

    public String getTechnicalHeadName() {
        return technicalHeadName;
    }

    public void setTechnicalHeadName(String technicalHeadName) {
        this.technicalHeadName = technicalHeadName;
    }

    public String getTechnicalHeadLicenseNumber() {
        return technicalHeadLicenseNumber;
    }

    public void setTechnicalHeadLicenseNumber(String technicalHeadLicenseNumber) {
        this.technicalHeadLicenseNumber = technicalHeadLicenseNumber;
    }

    public String getPrescribingLevel() {
        return prescribingLevel;
    }

    public void setPrescribingLevel(String prescribingLevel) {
        this.prescribingLevel = prescribingLevel;
    }

    public String getSpecialities() {
        return specialities;
    }

    public void setSpecialities(String specialities) {
        this.specialities = specialities;
    }

    public String getAccreditationNumber() {
        return accreditationNumber;
    }

    public void setAccreditationNumber(String accreditationNumber) {
        this.accreditationNumber = accreditationNumber;
    }

    public String getEclaimAuthorizationNumber() {
        return eclaimAuthorizationNumber;
    }

    public void setEclaimAuthorizationNumber(String eclaimAuthorizationNumber) {
        this.eclaimAuthorizationNumber = eclaimAuthorizationNumber;
    }

    public String getLevelOfCare() {
        return levelOfCare;
    }

    public void setLevelOfCare(String levelOfCare) {
        this.levelOfCare = levelOfCare;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Timestamp getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(Timestamp importedAt) {
        this.importedAt = importedAt;
    }
}
