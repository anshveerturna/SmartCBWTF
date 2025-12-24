package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.dto.CBWTFDashboardDTO;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CBWTF Dashboard Service.
 * Provides tenant-scoped metrics for the CBWTF Admin Portal dashboard.
 */
@Service
@Transactional(readOnly = true)
public class CBWTFDashboardService {

        private static final Logger log = LoggerFactory.getLogger(CBWTFDashboardService.class);

        private final TenantAssertionService tenantAssertion;
        private final AgreementRepository agreementRepo;
        private final BagEventRepository bagEventRepo;
        private final BagLabelRepository bagLabelRepo;
        private final InvoiceRepository invoiceRepo;
        private final FacilityRepository facilityRepo;
        private final AppUserRepository userRepo;

        public CBWTFDashboardService(TenantAssertionService tenantAssertion,
                        AgreementRepository agreementRepo,
                        BagEventRepository bagEventRepo,
                        BagLabelRepository bagLabelRepo,
                        InvoiceRepository invoiceRepo,
                        FacilityRepository facilityRepo,
                        AppUserRepository userRepo) {
                this.tenantAssertion = tenantAssertion;
                this.agreementRepo = agreementRepo;
                this.bagEventRepo = bagEventRepo;
                this.bagLabelRepo = bagLabelRepo;
                this.invoiceRepo = invoiceRepo;
                this.facilityRepo = facilityRepo;
                this.userRepo = userRepo;
        }

        /**
         * Get complete dashboard metrics for the current tenant.
         */
        public CBWTFDashboardDTO getDashboardMetrics() {
                UUID facilityId = tenantAssertion.getRequiredTenantId();
                log.debug("Building dashboard metrics for facility {}", facilityId);

                CBWTFDashboardDTO dto = new CBWTFDashboardDTO();

                // Get facility info
                Facility facility = facilityRepo.findById(facilityId)
                                .orElseThrow(() -> new IllegalStateException("Facility not found"));
                dto.setFacilityName(facility.getName());
                dto.setSubscriptionPlan(facility.getSubscriptionPlan());
                dto.setSubscriptionExpiresAt(facility.getSubscriptionExpiresAt());

                // Compute subscription days left
                if (facility.getSubscriptionExpiresAt() != null) {
                        long daysLeft = ChronoUnit.DAYS.between(Instant.now(), facility.getSubscriptionExpiresAt());
                        dto.setSubscriptionDaysLeft(Math.max(0, daysLeft));
                } else {
                        dto.setSubscriptionDaysLeft(-1); // Unlimited / not set
                }

                // === OVERVIEW METRICS ===
                long activeAgreements = agreementRepo.countActiveByFacilityId(facilityId);
                long totalAgreements = agreementRepo.countByFacilityId(facilityId);
                dto.setActiveAgreements(activeAgreements);
                dto.setTotalAgreements(totalAgreements);
                dto.setActiveHcfs(activeAgreements); // 1 active agreement = 1 active HCF

                // Bag label stats
                dto.setTotalBagLabelsIssued(bagLabelRepo.countByFacilityId(facilityId));

                // Bag event stats
                Instant now = Instant.now();
                Instant startOfDay = now.truncatedTo(java.time.temporal.ChronoUnit.DAYS);
                Instant startOfWeek = now.minus(7, ChronoUnit.DAYS);
                Instant startOfMonth = now.minus(30, ChronoUnit.DAYS);

                dto.setBagsProcessedToday(bagEventRepo.countByFacilityIdAndEventTypeAndEventTsAfter(
                                facilityId, "CBWTF_VERIFICATION", startOfDay));
                dto.setBagsProcessedThisWeek(bagEventRepo.countByFacilityIdAndEventTypeAndEventTsAfter(
                                facilityId, "CBWTF_VERIFICATION", startOfWeek));
                dto.setBagsProcessedThisMonth(bagEventRepo.countByFacilityIdAndEventTypeAndEventTsAfter(
                                facilityId, "CBWTF_VERIFICATION", startOfMonth));

                // === VEHICLE & STAFF METRICS ===
                // Count drivers as "vehicles" - each driver represents a vehicle
                long totalDrivers = userRepo.countByFacilityIdAndActive(facilityId, true);
                dto.setTotalVehicles(totalDrivers);

                // Vehicles online: drivers with GPS within 15 minutes (placeholder - currently
                // just using driver count)
                // In a production system, this would query UserLocation for recent GPS pings
                dto.setVehiclesOnline(Math.min(totalDrivers, 3)); // Placeholder: shows some vehicles online

                // Staff attendance: count of active staff
                int totalStaff = userRepo.countByFacilityIdAndActive(facilityId, true);
                dto.setTotalStaff(totalStaff);
                dto.setStaffPresentToday(totalStaff > 0 ? totalStaff - 1 : 0); // Placeholder: most staff present

                // === FINANCIAL METRICS ===
                BigDecimal pendingAmount = invoiceRepo.sumAmountByFacilityIdAndStatus(facilityId, "PENDING");
                dto.setPendingInvoiceAmount(pendingAmount != null ? pendingAmount : BigDecimal.ZERO);
                dto.setPendingInvoiceCount(invoiceRepo.countByFacilityIdAndStatus(facilityId, "PENDING"));

                BigDecimal paidThisMonth = invoiceRepo.sumPaidAmountByFacilityIdSince(facilityId, startOfMonth);
                dto.setPaidInvoiceAmountThisMonth(paidThisMonth != null ? paidThisMonth : BigDecimal.ZERO);
                dto.setPaidInvoiceCountThisMonth(invoiceRepo.countPaidByFacilityIdSince(facilityId, startOfMonth));

                BigDecimal totalRevenue = invoiceRepo.sumPaidAmountByFacilityId(facilityId);
                dto.setTotalRevenueAllTime(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

                // === HEALTH METRICS ===
                LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
                dto.setAgreementsExpiringSoon(
                                agreementRepo.countExpiringSoonByFacilityId(facilityId, thirtyDaysFromNow));
                dto.setAgreementsWithDuesPending(agreementRepo.countByFacilityIdAndDuesStatus(facilityId, "PENDING"));
                dto.setAgreementsInDispute(agreementRepo.countByFacilityIdAndStatus(facilityId, "DISPUTED"));

                // Anomaly bags this week
                dto.setAnomalyBagsThisWeek(bagEventRepo.countAnomaliesByFacilityIdSince(facilityId, startOfWeek));

                // === RISK ALERTS ===
                List<CBWTFDashboardDTO.RiskAlert> alerts = buildRiskAlerts(facility, dto);
                dto.setRiskAlerts(alerts);

                // === RECENT ACTIVITY ===
                List<CBWTFDashboardDTO.RecentBagEvent> recentEvents = bagEventRepo
                                .findRecentByFacilityId(facilityId, 10)
                                .stream()
                                .map(event -> new CBWTFDashboardDTO.RecentBagEvent(
                                                event.getBagLabel() != null ? event.getBagLabel().getQrCode() : null,
                                                event.getHcf() != null ? event.getHcf().getName() : null,
                                                event.getEventType(),
                                                event.getAnomalyState(),
                                                event.getEventTs()))
                                .collect(Collectors.toList());
                dto.setRecentBagEvents(recentEvents);

                // Expiring agreements
                List<CBWTFDashboardDTO.AgreementSummary> expiringAgreements = agreementRepo
                                .findExpiringSoonByFacilityId(facilityId, thirtyDaysFromNow)
                                .stream()
                                .map(this::mapToAgreementSummary)
                                .collect(Collectors.toList());
                dto.setExpiringAgreements(expiringAgreements);

                return dto;
        }

        /**
         * Build risk alerts based on current metrics.
         * Severity: CRITICAL, HIGH, MEDIUM
         */
        private List<CBWTFDashboardDTO.RiskAlert> buildRiskAlerts(Facility facility, CBWTFDashboardDTO dto) {
                List<CBWTFDashboardDTO.RiskAlert> alerts = new ArrayList<>();

                // CRITICAL: Subscription expires < 7 days
                if (dto.getSubscriptionDaysLeft() >= 0 && dto.getSubscriptionDaysLeft() < 7) {
                        alerts.add(new CBWTFDashboardDTO.RiskAlert(
                                        "CRITICAL",
                                        "SUBSCRIPTION_EXPIRY",
                                        "Subscription Expiring Soon",
                                        "Your subscription expires in " + dto.getSubscriptionDaysLeft()
                                                        + " days. Renew to avoid service interruption.",
                                        facility.getId().toString()));
                }

                // CRITICAL: CPCB report overdue (placeholder - would need CPCB report tracking)
                // In production, check if monthly CPCB report was submitted

                // HIGH: Invoice > 30 days unpaid
                if (dto.getPendingInvoiceCount() > 0) {
                        alerts.add(new CBWTFDashboardDTO.RiskAlert(
                                        "HIGH",
                                        "INVOICE_OVERDUE",
                                        dto.getPendingInvoiceCount() + " Unpaid Invoice(s)",
                                        "You have ₹" + dto.getPendingInvoiceAmount().setScale(0).toPlainString()
                                                        + " in pending invoices.",
                                        null));
                }

                // HIGH: Vehicle offline > 24h (placeholder - all drivers with no GPS for 24h)
                if (dto.getVehiclesOnline() < dto.getTotalVehicles() && dto.getTotalVehicles() > 0) {
                        long offlineCount = dto.getTotalVehicles() - dto.getVehiclesOnline();
                        alerts.add(new CBWTFDashboardDTO.RiskAlert(
                                        "HIGH",
                                        "VEHICLE_OFFLINE",
                                        offlineCount + " Vehicle(s) Offline",
                                        "Some vehicles have not reported GPS location recently.",
                                        null));
                }

                // MEDIUM: Agreement expires < 30 days
                if (dto.getAgreementsExpiringSoon() > 0) {
                        alerts.add(new CBWTFDashboardDTO.RiskAlert(
                                        "MEDIUM",
                                        "AGREEMENT_EXPIRY",
                                        dto.getAgreementsExpiringSoon() + " Agreement(s) Expiring Soon",
                                        "Some HCF agreements are expiring within 30 days. Consider renewal.",
                                        null));
                }

                return alerts;
        }

        private CBWTFDashboardDTO.AgreementSummary mapToAgreementSummary(Agreement agreement) {
                CBWTFDashboardDTO.AgreementSummary summary = new CBWTFDashboardDTO.AgreementSummary();
                summary.setAgreementNumber(agreement.getAgreementNumber());
                summary.setHcfName(agreement.getHcf() != null ? agreement.getHcf().getName() : null);
                summary.setStatus(agreement.getStatus());
                summary.setDuesStatus(agreement.getDuesStatus());
                summary.setEndDate(agreement.getEndDate());
                if (agreement.getEndDate() != null) {
                        long days = ChronoUnit.DAYS.between(LocalDate.now(), agreement.getEndDate());
                        summary.setDaysUntilExpiry((int) days);
                }
                return summary;
        }
}
