package com.ymall.backend.home.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.home.dto.HomeMerchandisingResponse;
import com.ymall.backend.home.service.HomeMerchandisingService;

@RestController
@RequestMapping("/api/home")
public class HomeMerchandisingController {

    private final HomeMerchandisingService merchandisingService;

    public HomeMerchandisingController(HomeMerchandisingService merchandisingService) {
        this.merchandisingService = merchandisingService;
    }

    @GetMapping("/merchandising")
    public ApiResponse<HomeMerchandisingResponse> getMerchandising() {
        return ApiResponse.success(merchandisingService.getMerchandising());
    }
}
