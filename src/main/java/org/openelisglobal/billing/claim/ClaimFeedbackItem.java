package org.openelisglobal.billing.claim;

import java.math.BigDecimal;

public class ClaimFeedbackItem {

    private Integer claimLineId;
    private BigDecimal approvedAmount;
    private String status;

    public Integer getClaimLineId() {
        return claimLineId;
    }

    public void setClaimLineId(Integer claimLineId) {
        this.claimLineId = claimLineId;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
