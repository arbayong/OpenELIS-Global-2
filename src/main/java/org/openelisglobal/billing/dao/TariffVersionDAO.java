package org.openelisglobal.billing.dao;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.billing.valueholder.TariffVersion;
import org.openelisglobal.common.dao.BaseDAO;

public interface TariffVersionDAO extends BaseDAO<TariffVersion, Integer> {

    /** Active tariff versions whose effective window covers the service date. */
    List<TariffVersion> getActiveVersionsCovering(Date serviceDate);
}
