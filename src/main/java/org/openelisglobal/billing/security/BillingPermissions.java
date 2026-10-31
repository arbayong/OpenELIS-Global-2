package org.openelisglobal.billing.security;

/**
 * The authority expressions guarding the billing endpoints.
 *
 * <p>
 * Each constant is a complete Spring Security expression, so an endpoint reads
 * {@code @PreAuthorize(BillingPermissions.VIEW_INVOICES)} rather than repeating
 * the permission key and its administrator fallback. The keys themselves are
 * seeded by the billing permission changelog and granted to roles there; the
 * Global Administrator role holds every one of them, so the fallback matters
 * only where an installation has edited the grants away in Role Management.
 *
 * <p>
 * The same keys gate the frontend routes through {@code SecureRoute permission=}
 * and the session payload's permission list, so a key seeded here works for both
 * the API and the UI without a second registration.
 */
public final class BillingPermissions {

    /** Seeded permission keys, shared with the role and menu changelogs. */
    public static final String KEY_VIEW_INVOICES = "billing.view.invoices";

    public static final String KEY_RECORD_PAYMENTS = "billing.record.payments";

    public static final String KEY_VIEW_REPORTS = "billing.view.reports";

    public static final String KEY_VIEW_CLAIMS = "billing.view.claims";

    public static final String KEY_MANAGE_CLAIMS = "billing.manage.claims";

    public static final String KEY_MANAGE_CONFIG = "billing.manage.config";

    private static final String OR_GLOBAL_ADMIN = " or hasRole('GLOBAL_ADMIN')";

    private static final String AUTHORITY = "hasAuthority('";

    public static final String VIEW_INVOICES = AUTHORITY + KEY_VIEW_INVOICES + "')" + OR_GLOBAL_ADMIN;

    public static final String RECORD_PAYMENTS = AUTHORITY + KEY_RECORD_PAYMENTS + "')" + OR_GLOBAL_ADMIN;

    public static final String VIEW_REPORTS = AUTHORITY + KEY_VIEW_REPORTS + "')" + OR_GLOBAL_ADMIN;

    public static final String VIEW_CLAIMS = AUTHORITY + KEY_VIEW_CLAIMS + "')" + OR_GLOBAL_ADMIN;

    /**
     * Building a claim batch and importing NHIA feedback change claim state, so
     * they carry the write key rather than the view key. A role that only reads
     * claims cannot submit them for payment.
     */
    public static final String MANAGE_CLAIMS = AUTHORITY + KEY_MANAGE_CLAIMS + "')" + OR_GLOBAL_ADMIN;

    /**
     * Reference data and gateway configuration hold payer accounts and payment
     * secrets, so this is the admin-only key of the set.
     */
    public static final String MANAGE_CONFIG = AUTHORITY + KEY_MANAGE_CONFIG + "')" + OR_GLOBAL_ADMIN;

    /**
     * Payment recording is gated on the write key alone: the compat grants give
     * Reception the counter role, and adding a role fallback here would grant
     * the bench roles write access the registry deliberately withholds.
     */
    private BillingPermissions() {
    }
}
