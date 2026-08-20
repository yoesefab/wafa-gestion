package com.wafabureau.gestion.exception;
import com.wafabureau.gestion.model.*;
import com.wafabureau.gestion.repository.*;
import com.wafabureau.gestion.service.*;
import com.wafabureau.gestion.enums.*;
import com.wafabureau.gestion.security.*;
import com.wafabureau.gestion.exception.*;
import com.wafabureau.gestion.dto.auth.*;
import com.wafabureau.gestion.dto.category.*;
import com.wafabureau.gestion.dto.product.*;
import com.wafabureau.gestion.dto.partner.*;
import com.wafabureau.gestion.dto.inventory.*;
import com.wafabureau.gestion.dto.sales.*;
import com.wafabureau.gestion.dto.purchase.*;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import com.wafabureau.gestion.util.TraceIds;

class ApiErrorFactoryTests {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void createsTheDocumentedErrorShape() {
        MDC.put(TraceIds.MDC_KEY, "test-trace-id");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products/999");

        ApiError error = ApiErrorFactory.create(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "Product not found.",
                request
        );

        assertThat(error.type()).isEqualTo("about:blank");
        assertThat(error.title()).isEqualTo("Not Found");
        assertThat(error.status()).isEqualTo(404);
        assertThat(error.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(error.detail()).isEqualTo("Product not found.");
        assertThat(error.instance()).isEqualTo("/api/products/999");
        assertThat(error.traceId()).isEqualTo("test-trace-id");
        assertThat(error.timestamp()).isNotNull();
        assertThat(error.fieldErrors()).isEmpty();
    }
}
