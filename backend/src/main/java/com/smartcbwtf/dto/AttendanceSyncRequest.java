package com.smartcbwtf.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request payload for batch attendance sync from mobile device.
 */
public class AttendanceSyncRequest {
    @Valid
    @NotEmpty
    private List<AttendanceSyncItem> events;

    public List<AttendanceSyncItem> getEvents() { return events; }
    public void setEvents(List<AttendanceSyncItem> events) { this.events = events; }
}
