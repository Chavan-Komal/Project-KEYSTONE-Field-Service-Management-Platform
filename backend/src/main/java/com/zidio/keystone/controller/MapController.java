package com.zidio.keystone.controller;

import com.zidio.keystone.dto.MapPinDto;
import com.zidio.keystone.service.MapService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/map")
@Tag(name = "Tracking Map")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/overview")
    public List<MapPinDto> overview() {
        return mapService.managerOverview();
    }

    @GetMapping("/my-requests")
    public List<MapPinDto> myRequests() {
        return mapService.customerOverview();
    }
}
