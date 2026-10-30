package org.openelisglobal.billing.dao;

import java.util.List;
import org.openelisglobal.billing.valueholder.BranchGatewayConfig;
import org.openelisglobal.common.dao.BaseDAO;

public interface BranchGatewayConfigDAO extends BaseDAO<BranchGatewayConfig, Integer> {

    List<BranchGatewayConfig> getEnabledByOrganizationId(Integer organizationId);
}
