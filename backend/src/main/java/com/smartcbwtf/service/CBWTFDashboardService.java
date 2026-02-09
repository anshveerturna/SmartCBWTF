package com.smartcbwtf.service;

import com.smartcbwtf.controller.CBWTFDashboardController;
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
                dto.setVehiclesOnline(totalDrivers);

                // Staff attendance: count of active staff
                int totalStaff = userRepo.countByFacilityIdAndActive(facilityId, true);
                dto.setTotalStaff(totalStaff);
                dto.setStaffPresentToday(totalStaff);

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
                long anomalyCount = bagEventRepo.countAnomaliesByFacilityIdSince(facilityId, startOfWeek);
                long missingVerificationCount = bagEventRepo.countMissingVerificationsByFacilitySince(facilityId,
                                startOfWeek);
                dto.setAnomalyBagsThisWeek(anomalyCount + missingVerificationCount);

                // === RISK ALERTS ===
                List<CBWTFDashboardDTO.RiskAlert> alerts = buildRiskAlerts(facility, dto, missingVerificationCount);
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
                                                event.getBagLabel() != null ? event.getBagLabel().getCategory()
                                                                : "UNKNOWN",
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
        private List<CBWTFDashboardDTO.RiskAlert> buildRiskAlerts(
                        Facility facility,
                        CBWTFDashboardDTO dto,
                        long missingVerificationCount) {
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

                if (dto.getVehiclesOnline() < dto.getTotalVehicles() && dto.getTotalVehicles() > 0) {
                        long offlineCount = dto.getTotalVehicles() - dto.getVehiclesOnline();
                        alerts.add(new CBWTFDashboardDTO.RiskAlert(
                                        "HIGH",
                                        "VEHICLE_OFFLINE",
                                        offlineCount + " Vehicle(s) Offline",
                                        "Some vehicles have not reported GPS location recently.",
                                        null));
                }

                if (missingVerificationCount > 0) {
                        alerts.add(new CBWTFDashboardDTO.RiskAlert(
                                        "HIGH",
                                        "UNVERIFIED_BAGS",
                                        missingVerificationCount + " Bag(s) Not Verified At CBWTF",
                                        "Collected bags pending CBWTF verification exceed expected SLA.",
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

        /**
         * Get category breakdown for pie chart.
         */
        public List<CBWTFDashboardController.CategoryBreakdown> getCategoryBreakdown() {
                try {
                        UUID facilityId = tenantAssertion.getRequiredTenantId();
                        Instant startOfWeek = Instant.now().minus(7, ChronoUnit.DAYS);

                        long yellowCount = bagEventRepo.countByFacilityIdAndWasteCategoryAndEventTsAfter(facilityId,
                                        "YELLOW",
                                        startOfWeek);
                        long redCount = bagEventRepo.countByFacilityIdAndWasteCategoryAndEventTsAfter(facilityId, "RED",
                                        startOfWeek);
                        long blueCount = bagEventRepo.countByFacilityIdAndWasteCategoryAndEventTsAfter(facilityId,
                                        "BLUE",
                                        startOfWeek);
                        long whiteCount = bagEventRepo.countByFacilityIdAndWasteCategoryAndEventTsAfter(facilityId,
                                        "WHITE",
                                        startOfWeek);

                        long total = yellowCount + redCount + blueCount + whiteCount;
                        if (total == 0) {
                                return List.of();
                        }

                        return List.of(
                                        new CBWTFDashboardController.CategoryBreakdown("Yellow", yellowCount,
                                                        "#FBBF24"),
                                        new CBWTFDashboardController.CategoryBreakdown("Red", redCount, "#EF4444"),
                                        new CBWTFDashboardController.CategoryBreakdown("Blue", blueCount, "#3B82F6"),
                                        new CBWTFDashboardController.CategoryBreakdown("White", whiteCount, "#94A3B8"));
                } catch (Exception e) {
                        log.warn("Error fetching category breakdown: {}", e.getMessage());
                        return List.of();
                }
        }

        /**
         * Get weekly trend for area chart.
         */
        public List<CBWTFDashboardController.WeeklyTrend> getWeeklyTrend() {
                try {
                        UUID facilityId = tenantAssertion.getRequiredTenantId();
                        List<CBWTFDashboardController.WeeklyTrend> trends = new ArrayList<>();
                        String[] dayNames = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

                        for (int i = 6; i >= 0; i--) {
                                Instant dayStart = Instant.now().minus(i, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
                                Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);

                                long yellow = bagEventRepo.countByFacilityIdAndWasteCategoryBetween(facilityId,
                                                "YELLOW",
                                                dayStart, dayEnd);
                                long red = bagEventRepo.countByFacilityIdAndWasteCategoryBetween(facilityId, "RED",
                                                dayStart,
                                                dayEnd);
                                long blue = bagEventRepo.countByFacilityIdAndWasteCategoryBetween(facilityId, "BLUE",
                                                dayStart,
                                                dayEnd);
                                long white = bagEventRepo.countByFacilityIdAndWasteCategoryBetween(facilityId, "WHITE",
                                                dayStart, dayEnd);

                                int dayOfWeek = java.time.ZonedDateTime
                                                .ofInstant(dayStart, java.time.ZoneId.systemDefault())
                                                .getDayOfWeek().getValue() % 7;
                                trends.add(new CBWTFDashboardController.WeeklyTrend(dayNames[dayOfWeek], yellow, red,
                                                blue,
                                                white));
                        }

                        return trends;
                } catch (Exception e) {
                        log.warn("Error fetching weekly trend: {}", e.getMessage());
                        return List.of();
                }
        }

        /**
         * Get yesterday vs today comparison.
         */
        public java.util.Map<String, Object> getTrendComparison() {
                try {
                        UUID facilityId = tenantAssertion.getRequiredTenantId();
                        Instant todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
                        Instant yesterdayStart = todayStart.minus(1, ChronoUnit.DAYS);

                        long todayBags = bagEventRepo.countByFacilityIdAndEventTypeAndEventTsAfter(facilityId,
                                        "CBWTF_VERIFICATION", todayStart);
                        long yesterdayBags = bagEventRepo.countByFacilityIdAndEventTypeBetween(facilityId,
                                        "CBWTF_VERIFICATION",
                                        yesterdayStart, todayStart);

                        double percentChange = 0;
                        if (yesterdayBags > 0) {
                                percentChange = ((double) (todayBags - yesterdayBags) / yesterdayBags) * 100;
                        } else if (todayBags > 0) {
                                percentChange = 100;
                        }

                        return java.util.Map.of(
                                        "todayBags", todayBags,
                                        "yesterdayBags", yesterdayBags,
                                        "percentChange", Math.round(percentChange),
                                        "isPositive", percentChange >= 0);
                } catch (Exception e) {
                        log.warn("Error fetching trend comparison: {}", e.getMessage());
                        return java.util.Map.of(
                                        "todayBags", 0L,
                                        "yesterdayBags", 0L,
                                        "percentChange", 0L,
                                        "isPositive", true);
                }
        }

        /**
         * Get anomaly bags from this week with full details.
         */
        public List<CBWTFDashboardController.AnomalyBagDTO> getAnomalyBags() {
                try {
                        UUID facilityId = tenantAssertion.getRequiredTenantId();
                        Instant weekStart = Instant.now().minus(7, ChronoUnit.DAYS);

                        // Get all bag events with anomalies from this week
                        List<CBWTFDashboardController.AnomalyBagDTO> anomalyEvents = bagEventRepo
                                        .findByFacilityIdAndEventTsBetween(
                                                        facilityId, weekStart, Instant.now())
                                        .stream()
                                        .filter(event -> event.getAnomalyState() != null
                                                        && !"OK".equals(event.getAnomalyState()))
                                        .map(event -> toAnomalyDto(event, event.getAnomalyState()))
                                        .collect(Collectors.toList());

                        List<CBWTFDashboardController.AnomalyBagDTO> missingVerificationBags = bagEventRepo
                                        .findMissingBags(facilityId, weekStart)
                                        .stream()
                                        .map(event -> toAnomalyDto(event, "NOT_VERIFIED_AT_CBWTF"))
                                        .collect(Collectors.toList());
                        anomalyEvents.addAll(missingVerificationBags);
                        return anomalyEvents;
                } catch (Exception e) {
                        log.warn("Error fetching anomaly bags: {}", e.getMessage());
                        return new ArrayList<>();
                }
        }

        private CBWTFDashboardController.AnomalyBagDTO toAnomalyDto(
                        com.smartcbwtf.domain.BagEvent event,
                        String anomalyState) {
                String hcfName = event.getHcf() != null ? event.getHcf().getName() : "Unknown";
                String category = event.getBagLabel() != null
                                ? event.getBagLabel().getCategory()
                                : "Unknown";
                String staffName = null;
                if (event.getCollectedByUserId() != null) {
                        staffName = userRepo.findById(event.getCollectedByUserId())
                                        .map(u -> u.getFullName() != null ? u.getFullName() : u.getUsername())
                                        .orElse(null);
                }

                return new CBWTFDashboardController.AnomalyBagDTO(
                                event.getId().toString(),
                                event.getEventTs().toString(),
                                hcfName,
                                category,
                                anomalyState,
                                event.getWeightKg() != null ? event.getWeightKg().doubleValue() : null,
                                event.getCollectedByUserId() != null ? event.getCollectedByUserId().toString() : null,
                                staffName,
                                event.getGpsLat(),
                                event.getGpsLon(),
                                event.getEventType());
        }
}
