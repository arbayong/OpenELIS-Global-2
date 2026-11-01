package org.openelisglobal.billing.claim.claimit;

import java.util.List;

/**
 * A single issue (error or warning) reported by CLAIM-it for an imported claim.
 */
public class ClaimitIssue {

    private String code;
    private String info;
    private String help;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }
}
