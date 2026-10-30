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

@Entity
@Table(name = "patient_policy")
public class PatientPolicy extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "patient_policy_generator")
    @SequenceGenerator(name = "patient_policy_generator", sequenceName = "patient_policy_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "patient_id", length = 10)
    private String patientId;

    @Column(name = "payer_id")
    private Integer payerId;

    @Column(name = "member_number", length = 60)
    private String memberNumber;

    @Column(name = "mother_member_number", length = 60)
    private String motherMemberNumber;

    @Column(name = "effective_from")
    private Date effectiveFrom;

    @Column(name = "effective_to")
    private Date effectiveTo;

    @Column(name = "active", length = 1)
    private String active;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Integer getPayerId() {
        return payerId;
    }

    public void setPayerId(Integer payerId) {
        this.payerId = payerId;
    }

    public String getMemberNumber() {
        return memberNumber;
    }

    public void setMemberNumber(String memberNumber) {
        this.memberNumber = memberNumber;
    }

    public String getMotherMemberNumber() {
        return motherMemberNumber;
    }

    public void setMotherMemberNumber(String motherMemberNumber) {
        this.motherMemberNumber = motherMemberNumber;
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

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }
}
