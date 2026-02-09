package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.domain.QrLabelOrder.QrOrderStatus;
import com.smartcbwtf.domain.QrLabelOrder.QrOrderType;
import com.smartcbwtf.dto.LabelIssueRequest;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * QR Label Order Service - Handles chargeable QR label requests.
 * 
 * Pricing:
 * - HCF_SELF: Cheaper rate (HCF generates on their portal)
 * - CBWTF_REQUEST: Higher rate (HCF requests from CBWTF)
 * 
 * Charges are tracked in the QR order itself and can be added to monthly
 * billing.
 */
@Service
public class QrOrderService {

        private static final Logger log = LoggerFactory.getLogger(QrOrderService.class);

        private final QrLabelOrderRepository qrOrderRepository;
        private final HcfRepository hcfRepository;
        private final FacilityRepository facilityRepository;
        private final AgreementRepository agreementRepository;
        private final SystemConfigService configService;
        private final LabelService labelService;
        private final AuditLogService auditLogService;

        // Default pricing (can be overridden via config)
        private static final BigDecimal DEFAULT_SELF_PRICE = new BigDecimal("5.00");
        private static final BigDecimal DEFAULT_CBWTF_PRICE = new BigDecimal("5.00");
        private static final int DEFAULT_MAX_QUANTITY = 500;

        public QrOrderService(
                        QrLabelOrderRepository qrOrderRepository,
                        HcfRepository hcfRepository,
                        FacilityRepository facilityRepository,
                        AgreementRepository agreementRepository,
                        SystemConfigService configService,
                        LabelService labelService,
                        AuditLogService auditLogService) {
                this.qrOrderRepository = qrOrderRepository;
                this.hcfRepository = hcfRepository;
                this.facilityRepository = facilityRepository;
                this.agreementRepository = agreementRepository;
                this.configService = configService;
                this.labelService = labelService;
                this.auditLogService = auditLogService;
        }

        /**
         * Get QR pricing for display.
         */
        public QrPricing getPricing() {
                BigDecimal selfPrice = getConfigPrice("qr.price.hcf_self_per_unit", DEFAULT_SELF_PRICE);
                BigDecimal requestPrice = getConfigPrice("qr.price.cbwtf_request_per_unit", DEFAULT_CBWTF_PRICE);
                int maxQty = configService.getInt("qr.max_quantity_per_order", DEFAULT_MAX_QUANTITY);
                return new QrPricing(selfPrice, requestPrice, maxQty);
        }

        private BigDecimal getConfigPrice(String key, BigDecimal defaultValue) {
                String value = configService.getString(key, defaultValue.toString());
                try {
                        return new BigDecimal(value);
                } catch (NumberFormatException e) {
                        return defaultValue;
                }
        }

        /**
         * HCF requests QR labels from CBWTF.
         */
        @Transactional
        public QrLabelOrder createCbwtfRequest(UUID hcfId, String category, int quantity, String notes) {
                validateQuantity(quantity);

                Hcf hcf = hcfRepository.findById(hcfId)
                                .orElseThrow(() -> new IllegalArgumentException("HCF not found"));

                // Find active agreement to identify the facility
                Agreement agreement = agreementRepository.findActiveByHcfId(hcfId)
                                .orElseThrow(() -> new IllegalStateException("No active agreement found for this HCF"));

                Facility facility = agreement.getFacility();

                BigDecimal unitPrice = getConfigPrice("qr.price.cbwtf_request_per_unit", DEFAULT_CBWTF_PRICE);
                BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

                QrLabelOrder order = new QrLabelOrder();
                order.setHcf(hcf);
                order.setFacility(facility);
                order.setAgreement(agreement);
                order.setWasteCategory(category);
                order.setQuantity(quantity);
                order.setUnitPrice(unitPrice);
                order.setTotalAmount(totalAmount);
                order.setOrderType(QrOrderType.CBWTF_REQUEST);
                order.setStatus(QrOrderStatus.PENDING);
                order.setNotes(notes);
                order.setRequestedAt(Instant.now());

                QrLabelOrder saved = qrOrderRepository.save(order);

                auditLogService.log("QR_ORDER", saved.getId(), "QR_REQUEST_CREATED", null,
                                String.format("HCF requested %d %s QR labels from CBWTF. Total: ₹%.2f",
                                                quantity, category, totalAmount));

                log.info("QR request created: orderId={}, hcfId={}, category={}, quantity={}, total={}",
                                saved.getId(), hcfId, category, quantity, totalAmount);

                return saved;
        }

        /**
         * HCF self-generates QR labels.
         * Charges are tracked in the order for later billing.
         */
        @Transactional
        public QrSelfGenerateResult selfGenerate(UUID hcfId, String category, int quantity,
                        java.time.LocalDate validUntil) {
                validateQuantity(quantity);
                validateValidity(validUntil);

                Hcf hcf = hcfRepository.findById(hcfId)
                                .orElseThrow(() -> new IllegalArgumentException("HCF not found"));

                // Find active agreement to identify the facility
                Agreement agreement = agreementRepository.findActiveByHcfId(hcfId)
                                .orElseThrow(() -> new IllegalStateException("No active agreement found for this HCF"));

                Facility facility = agreement.getFacility();

                BigDecimal unitPrice = getConfigPrice("qr.price.hcf_self_per_unit", DEFAULT_SELF_PRICE);
                BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

                // Create order record (charges tracked for billing)
                QrLabelOrder order = new QrLabelOrder();
                order.setHcf(hcf);
                order.setFacility(facility);
                order.setAgreement(agreement);
                order.setWasteCategory(category);
                order.setQuantity(quantity);
                order.setUnitPrice(unitPrice);
                order.setTotalAmount(totalAmount);
                order.setOrderType(QrOrderType.HCF_SELF);
                order.setStatus(QrOrderStatus.FULFILLED);
                order.setRequestedAt(Instant.now());
                order.setFulfilledAt(Instant.now());

                // Generate labels using existing LabelService
                LabelIssueRequest labelRequest = new LabelIssueRequest();
                labelRequest.setHcfId(hcfId);
                labelRequest.setFacilityId(facility.getId());
                labelRequest.setCategory(category);
                labelRequest.setQuantity(quantity);
                var labelResponse = labelService.issue(labelRequest);

                order.setPdfUrl(labelResponse.getPdfUrl());
                QrLabelOrder saved = qrOrderRepository.save(order);

                auditLogService.log("QR_ORDER", saved.getId(), "QR_SELF_GENERATED", null,
                                String.format("HCF self-generated %d %s QR labels. Charge: ₹%.2f",
                                                quantity, category, totalAmount));

                log.info("QR self-generated: orderId={}, hcfId={}, category={}, quantity={}, charge={}",
                                saved.getId(), hcfId, category, quantity, totalAmount);

                return new QrSelfGenerateResult(saved, labelResponse.getQrCodes(), labelResponse.getPdfUrl());
        }

        /**
         * CBWTF admin directly generates QR labels for an HCF.
         * No charge to HCF (admin-initiated generation).
         */
        @Transactional
        public QrSelfGenerateResult adminDirectGenerate(UUID hcfId, String category, int quantity, UUID adminUserId,
                        java.time.LocalDate validUntil) {
                validateQuantity(quantity);
                validateValidity(validUntil);

                Hcf hcf = hcfRepository.findById(hcfId)
                                .orElseThrow(() -> new IllegalArgumentException("HCF not found"));

                Agreement agreement = agreementRepository.findActiveByHcfId(hcfId)
                                .orElseThrow(() -> new IllegalStateException("No active agreement found for this HCF"));

                Facility facility = agreement.getFacility();

                // Admin-generated labels are free (no charge to HCF)
                QrLabelOrder order = new QrLabelOrder();
                order.setHcf(hcf);
                order.setFacility(facility);
                order.setAgreement(agreement);
                order.setWasteCategory(category);
                order.setQuantity(quantity);
                order.setUnitPrice(BigDecimal.ZERO);
                order.setTotalAmount(BigDecimal.ZERO);
                order.setOrderType(QrOrderType.CBWTF_REQUEST);
                order.setStatus(QrOrderStatus.FULFILLED);
                order.setRequestedAt(Instant.now());
                order.setFulfilledAt(Instant.now());
                order.setFulfilledBy(adminUserId);
                order.setNotes("Admin direct generation");

                // Generate labels using existing LabelService
                LabelIssueRequest labelRequest = new LabelIssueRequest();
                labelRequest.setHcfId(hcfId);
                labelRequest.setFacilityId(facility.getId());
                labelRequest.setCategory(category);
                labelRequest.setQuantity(quantity);
                labelRequest.setValidUntil(validUntil);
                var labelResponse = labelService.issue(labelRequest);

                order.setPdfUrl(labelResponse.getPdfUrl());
                QrLabelOrder saved = qrOrderRepository.save(order);

                auditLogService.log("QR_ORDER", saved.getId(), "QR_ADMIN_GENERATED", adminUserId,
                                String.format("CBWTF admin generated %d %s QR labels for HCF %s (no charge)",
                                                quantity, category, hcf.getName()));

                log.info("QR admin-generated: orderId={}, hcfId={}, category={}, quantity={}, admin={}",
                                saved.getId(), hcfId, category, quantity, adminUserId);

                return new QrSelfGenerateResult(saved, labelResponse.getQrCodes(), labelResponse.getPdfUrl());
        }

        /**
         * HCF self-generates QR labels for multiple categories in a single PDF.
         */
        @Transactional
        public QrSelfGenerateResult selfGenerateMulti(UUID hcfId, Map<String, Integer> categoryQuantities,
                        java.time.LocalDate validUntil) {
                int totalQuantity = categoryQuantities.values().stream().mapToInt(Integer::intValue).sum();
                validateQuantity(totalQuantity);
                validateValidity(validUntil);

                Hcf hcf = hcfRepository.findById(hcfId)
                                .orElseThrow(() -> new IllegalArgumentException("HCF not found"));
                Agreement agreement = agreementRepository.findActiveByHcfId(hcfId)
                                .orElseThrow(() -> new IllegalStateException("No active agreement found for this HCF"));
                Facility facility = agreement.getFacility();

                BigDecimal unitPrice = getConfigPrice("qr.price.hcf_self_per_unit", DEFAULT_SELF_PRICE);
                BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(totalQuantity));

                QrLabelOrder order = new QrLabelOrder();
                order.setHcf(hcf);
                order.setFacility(facility);
                order.setAgreement(agreement);
                order.setWasteCategory("MULTI");
                order.setQuantity(totalQuantity);
                order.setUnitPrice(unitPrice);
                order.setTotalAmount(totalAmount);
                order.setOrderType(QrOrderType.HCF_SELF);
                order.setStatus(QrOrderStatus.FULFILLED);
                order.setRequestedAt(Instant.now());
                order.setFulfilledAt(Instant.now());

                var labelResponse = labelService.issueMultiCategory(hcfId, facility.getId(), categoryQuantities,
                                validUntil);

                order.setPdfUrl(labelResponse.getPdfUrl());
                QrLabelOrder saved = qrOrderRepository.save(order);

                auditLogService.log("QR_ORDER", saved.getId(), "QR_SELF_GENERATED_MULTI", null,
                                String.format("HCF self-generated %d QR labels across %d categories. Charge: ₹%.2f",
                                                totalQuantity, categoryQuantities.size(), totalAmount));

                log.info("QR self-generated multi: orderId={}, hcfId={}, categories={}, totalQty={}, charge={}",
                                saved.getId(), hcfId, categoryQuantities.keySet(), totalQuantity, totalAmount);

                return new QrSelfGenerateResult(saved, labelResponse.getQrCodes(), labelResponse.getPdfUrl());
        }

        /**
         * CBWTF admin directly generates QR labels for an HCF across multiple
         * categories.
         * No charge to HCF (admin-initiated generation).
         */
        @Transactional
        public QrSelfGenerateResult adminDirectGenerateMulti(UUID hcfId, Map<String, Integer> categoryQuantities,
                        UUID adminUserId, java.time.LocalDate validUntil) {
                int totalQuantity = categoryQuantities.values().stream().mapToInt(Integer::intValue).sum();
                validateQuantity(totalQuantity);
                validateValidity(validUntil);

                Hcf hcf = hcfRepository.findById(hcfId)
                                .orElseThrow(() -> new IllegalArgumentException("HCF not found"));
                Agreement agreement = agreementRepository.findActiveByHcfId(hcfId)
                                .orElseThrow(() -> new IllegalStateException("No active agreement found for this HCF"));
                Facility facility = agreement.getFacility();

                QrLabelOrder order = new QrLabelOrder();
                order.setHcf(hcf);
                order.setFacility(facility);
                order.setAgreement(agreement);
                order.setWasteCategory("MULTI");
                order.setQuantity(totalQuantity);
                order.setUnitPrice(BigDecimal.ZERO);
                order.setTotalAmount(BigDecimal.ZERO);
                order.setOrderType(QrOrderType.CBWTF_REQUEST);
                order.setStatus(QrOrderStatus.FULFILLED);
                order.setRequestedAt(Instant.now());
                order.setFulfilledAt(Instant.now());
                order.setFulfilledBy(adminUserId);
                order.setNotes("Admin direct generation (multi-category)");

                var labelResponse = labelService.issueMultiCategory(hcfId, facility.getId(), categoryQuantities,
                                validUntil);

                order.setPdfUrl(labelResponse.getPdfUrl());
                QrLabelOrder saved = qrOrderRepository.save(order);

                auditLogService.log("QR_ORDER", saved.getId(), "QR_ADMIN_GENERATED_MULTI", adminUserId,
                                String.format("CBWTF admin generated %d QR labels across %d categories for HCF %s (no charge)",
                                                totalQuantity, categoryQuantities.size(), hcf.getName()));

                log.info("QR admin-generated multi: orderId={}, hcfId={}, categories={}, totalQty={}, admin={}",
                                saved.getId(), hcfId, categoryQuantities.keySet(), totalQuantity, adminUserId);

                return new QrSelfGenerateResult(saved, labelResponse.getQrCodes(), labelResponse.getPdfUrl());
        }

        /**
         * CBWTF admin fulfills a QR request.
         */
        @Transactional
        public QrLabelOrder fulfillRequest(UUID orderId, UUID adminUserId) {
                QrLabelOrder order = qrOrderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

                if (order.getStatus() != QrOrderStatus.PENDING) {
                        throw new IllegalStateException("Order is not pending: " + order.getStatus());
                }

                // Generate labels
                LabelIssueRequest labelRequest = new LabelIssueRequest();
                labelRequest.setHcfId(order.getHcf().getId());
                labelRequest.setFacilityId(order.getFacility().getId());
                labelRequest.setCategory(order.getWasteCategory());
                labelRequest.setQuantity(order.getQuantity());
                var labelResponse = labelService.issue(labelRequest);

                order.setStatus(QrOrderStatus.FULFILLED);
                order.setFulfilledAt(Instant.now());
                order.setFulfilledBy(adminUserId);
                order.setPdfUrl(labelResponse.getPdfUrl());

                auditLogService.log("QR_ORDER", orderId, "QR_REQUEST_FULFILLED", adminUserId,
                                String.format("CBWTF fulfilled %d %s QR labels for HCF %s. Charge: ₹%.2f",
                                                order.getQuantity(), order.getWasteCategory(),
                                                order.getHcf().getName(), order.getTotalAmount()));

                log.info("QR request fulfilled: orderId={}, adminUserId={}", orderId, adminUserId);

                return qrOrderRepository.save(order);
        }

        /**
         * CBWTF admin rejects a QR request.
         */
        @Transactional
        public QrLabelOrder rejectRequest(UUID orderId, UUID adminUserId, String reason) {
                QrLabelOrder order = qrOrderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

                if (order.getStatus() != QrOrderStatus.PENDING) {
                        throw new IllegalStateException("Order is not pending");
                }

                order.setStatus(QrOrderStatus.REJECTED);
                order.setNotes((order.getNotes() != null ? order.getNotes() + "\n" : "") + "Rejected: " + reason);

                auditLogService.log("QR_ORDER", orderId, "QR_REQUEST_REJECTED", adminUserId, reason);

                return qrOrderRepository.save(order);
        }

        /**
         * List pending orders for CBWTF admin.
         */
        public List<QrLabelOrder> listPendingOrders(UUID facilityId) {
                return qrOrderRepository.findPendingOrdersByFacility(facilityId);
        }

        /**
         * List all orders for a facility.
         */
        public List<QrLabelOrder> listAllOrders(UUID facilityId) {
                return qrOrderRepository.findByFacilityIdOrderByRequestedAtDesc(facilityId);
        }

        /**
         * List all orders for an HCF.
         */
        public List<QrLabelOrder> listHcfOrders(UUID hcfId) {
                return qrOrderRepository.findByHcfIdOrderByRequestedAtDesc(hcfId);
        }

        // Helper methods
        private void validateQuantity(int quantity) {
                if (quantity <= 0) {
                        throw new IllegalArgumentException("Quantity must be positive");
                }
                int maxQty = configService.getInt("qr.max_quantity_per_order", DEFAULT_MAX_QUANTITY);
                if (quantity > maxQty) {
                        throw new IllegalArgumentException("Maximum " + maxQty + " QR labels per order");
                }
        }

        private void validateValidity(java.time.LocalDate validUntil) {
                if (validUntil == null)
                        return;
                java.time.LocalDate today = java.time.LocalDate.now();
                if (validUntil.isBefore(today)) {
                        throw new IllegalArgumentException("Validity date cannot be in the past");
                }
                if (validUntil.isAfter(today.plusDays(31))) {
                        throw new IllegalArgumentException("Validity cannot exceed 1 month from today");
                }
        }

        // DTOs
        public record QrPricing(BigDecimal selfGeneratePrice, BigDecimal cbwtfRequestPrice, int maxQuantity) {
        }

        public record QrSelfGenerateResult(QrLabelOrder order, List<String> qrCodes, String pdfUrl) {
        }
}
