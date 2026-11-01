package org.openelisglobal.billing.daoimpl;

import org.openelisglobal.billing.dao.ClaimitSettingsDAO;
import org.openelisglobal.billing.valueholder.ClaimitSettings;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ClaimitSettingsDAOImpl extends BaseDAOImpl<ClaimitSettings, Integer> implements ClaimitSettingsDAO {

    public ClaimitSettingsDAOImpl() {
        super(ClaimitSettings.class);
    }
}
