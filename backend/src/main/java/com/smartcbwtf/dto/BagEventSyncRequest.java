package com.smartcbwtf.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BagEventSyncRequest {
    @Valid
    @NotEmpty
    private List<BagEventSyncItem> events;

    public List<BagEventSyncItem> getEvents() { return events; }
    public void setEvents(List<BagEventSyncItem> events) { this.events = events; }
}
