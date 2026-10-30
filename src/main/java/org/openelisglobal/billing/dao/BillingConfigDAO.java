package org.openelisglobal.billing.dao;

import java.util.List;
import org.openelisglobal.billing.valueholder.BillingConfig;
import org.openelisglobal.common.dao.BaseDAO;

public interface BillingConfigDAO extends BaseDAO<BillingConfig, Integer> {

    List<BillingConfig> getByConfigKey(String configKey);
}
