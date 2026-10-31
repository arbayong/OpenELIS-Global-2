package org.openelisglobal.billing.dao;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.billing.valueholder.GdrgMapping;
import org.openelisglobal.common.dao.BaseDAO;

public interface GdrgMappingDAO extends BaseDAO<GdrgMapping, Integer> {

    /** Active mapping for a local code, optionally honoring the effective window. */
    List<GdrgMapping> getActiveByLocalCode(String localCode, Date onDate);
}
