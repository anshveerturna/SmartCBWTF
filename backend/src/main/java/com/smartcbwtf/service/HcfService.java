package com.smartcbwtf.service;

import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.HcfRegistrationRequest;
import com.smartcbwtf.dto.HcfRegistrationResponse;
import com.smartcbwtf.repository.HcfRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class HcfService {

    private final HcfRepository hcfRepository;
    private final AuditLogService auditLogService;

    public HcfService(HcfRepository hcfRepository, AuditLogService auditLogService) {
        this.hcfRepository = hcfRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public HcfRegistrationResponse register(HcfRegistrationRequest request) {
        Hcf hcf = new Hcf();
        hcf.setCode("HCF-" + UUID.randomUUID());
        hcf.setName(request.getName());
        hcf.setAddress(request.getAddress());
        hcf.setContactEmail(request.getContactEmail());
        hcf.setContactPhone(request.getContactPhone());
        hcf.setNumberOfBeds(request.getNumberOfBeds());
        hcf.setGpsLat(request.getGpsLat());
        hcf.setGpsLon(request.getGpsLon());
        hcf.setStatus("PENDING_APPROVAL");
        hcfRepository.save(hcf);
        auditLogService.log("HCF", hcf.getId(), "REGISTER", null, null);
        return new HcfRegistrationResponse("PENDING_APPROVAL", hcf.getId(), hcf.getCode());
    }

    public List<Hcf> listPending() {
        return hcfRepository.findAll().stream()
                .filter(h -> "PENDING_APPROVAL".equalsIgnoreCase(h.getStatus()))
                .toList();
    }
}
