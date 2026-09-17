package com.zidio.keystone.dto;

import java.util.UUID;

// A single point on the tracking map — either a technician's base location
// or an open work order's site. `kind` tells the frontend which icon/style
// to render; the work-order-only fields are null for a technician pin.
public record MapPinDto(
    String kind, // "TECHNICIAN" | "WORK_ORDER"
    UUID id,
    String label,
    double latitude,
    double longitude,
    String status,
    String priority,
    String assignedToName
) {
    public static MapPinDto technician(UUID id, String name, double lat, double lng) {
        return new MapPinDto("TECHNICIAN", id, name, lat, lng, null, null, null);
    }

    public static MapPinDto workOrder(UUID id, String label, double lat, double lng, String status, String priority, String assignedToName) {
        return new MapPinDto("WORK_ORDER", id, label, lat, lng, status, priority, assignedToName);
    }

    public static MapPinDto site(UUID id, String name, double lat, double lng) {
        return new MapPinDto("SITE", id, name, lat, lng, null, null, null);
    }
}
