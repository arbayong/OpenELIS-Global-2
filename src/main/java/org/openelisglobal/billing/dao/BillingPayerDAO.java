package org.openelisglobal.billing.dao;

import java.util.List;
import org.openelisglobal.billing.valueholder.BillingPayer;
import org.openelisglobal.common.dao.BaseDAO;

public interface BillingPayerDAO extends BaseDAO<BillingPayer, Integer> {

    List<BillingPayer> getActiveByType(String type);
}
