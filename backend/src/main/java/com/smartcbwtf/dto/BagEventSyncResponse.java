package com.smartcbwtf.dto;

import java.util.List;

public class BagEventSyncResponse {
    public static class Ack {
        private String qrCode;
        private String status;
        private String message;

        public Ack(String qrCode, String status, String message) {
            this.qrCode = qrCode;
            this.status = status;
            this.message = message;
        }

        public String getQrCode() { return qrCode; }
        public void setQrCode(String qrCode) { this.qrCode = qrCode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    private List<Ack> acks;

    public BagEventSyncResponse(List<Ack> acks) {
        this.acks = acks;
    }

    public List<Ack> getAcks() { return acks; }
    public void setAcks(List<Ack> acks) { this.acks = acks; }
}
