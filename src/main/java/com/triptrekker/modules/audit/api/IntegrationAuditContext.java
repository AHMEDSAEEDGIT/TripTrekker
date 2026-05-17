package com.triptrekker.modules.audit.api;

import java.util.EnumSet;
import java.util.Set;

/**
 * Thread-local opt-in gate for selective integration auditing.
 *
 * <p>Each vendor is tracked independently, so multiple vendors can be active
 * simultaneously within the same thread (e.g. a booking flow that calls both
 * Duffel and Stripe). The {@code AuditingRestClientInterceptor} is constructed
 * with a fixed vendor and checks {@link #isActive(IntegrationVendor)} — it
 * never touches another vendor's state.
 *
 * <p>Typical usage:
 * <pre>
 *   IntegrationAuditContext.begin(IntegrationVendor.DUFFEL);
 *   try {
 *       duffelClient.createOrder(request);
 *   } finally {
 *       IntegrationAuditContext.end(IntegrationVendor.DUFFEL);
 *   }
 * </pre>
 */
public final class IntegrationAuditContext {

    private static final ThreadLocal<Set<IntegrationVendor>> ACTIVE_VENDORS =
            ThreadLocal.withInitial(() -> EnumSet.noneOf(IntegrationVendor.class));

    private IntegrationAuditContext() {}

    public static void begin(IntegrationVendor vendor) {
        ACTIVE_VENDORS.get().add(vendor);
    }

    public static boolean isActive(IntegrationVendor vendor) {
        return ACTIVE_VENDORS.get().contains(vendor);
    }

    public static void end(IntegrationVendor vendor) {
        Set<IntegrationVendor> vendors = ACTIVE_VENDORS.get();
        vendors.remove(vendor);
        if (vendors.isEmpty()) {
            ACTIVE_VENDORS.remove();
        }
    }

    public static void clearAll() {
        ACTIVE_VENDORS.remove();
    }
}
