package org.openelisglobal.billing.dao;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.billing.valueholder.GdrgMapping;
import org.openelisglobal.common.dao.BaseDAO;

public interface GdrgMappingDAO extends BaseDAO<GdrgMapping, Integer> {

    /** Active mapping for a local code, optionally honoring the effective window and a tariff version. */
    List<GdrgMapping> getActiveByLocalCode(String localCode, Date onDate, Integer versionId);

    /** Convenience overload: any version. */
    default List<GdrgMapping> getActiveByLocalCode(String localCode, Date onDate) {
        return getActiveByLocalCode(localCode, onDate, null);
    }
}
