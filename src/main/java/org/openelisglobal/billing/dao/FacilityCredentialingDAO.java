package org.openelisglobal.billing.dao;

import java.util.List;
import org.openelisglobal.billing.valueholder.FacilityCredentialing;
import org.openelisglobal.common.dao.BaseDAO;

public interface FacilityCredentialingDAO extends BaseDAO<FacilityCredentialing, Integer> {

    List<FacilityCredentialing> getByOrganizationId(Integer organizationId);
}
