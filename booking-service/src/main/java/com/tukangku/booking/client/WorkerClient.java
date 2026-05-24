package com.tukangku.booking.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Client untuk berkomunikasi dengan nurse-service (worker-service).
 * Semua komunikasi service-to-service langsung (tidak melalui API Gateway).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkerClient {

    private final RestTemplate restTemplate;

    @Value("${worker.service.url:http://localhost:8082}")
    private String workerServiceUrl;

    /**
     * Panggil endpoint internal di worker-service yang secara atomik:
     * 1. Validasi workStatus == OPEN
     * 2. Set workStatus = BOOKED, isAvailable = false
     * 3. Return info tukang (id, name, avatar)
     *
     * @throws IllegalArgumentException jika tukang tidak ditemukan (404)
     * @throws IllegalStateException    jika tukang BOOKED/CLOSED (409)
     */
    public WorkerInfoDto bookWorker(String workerId) {
        String url = workerServiceUrl + "/internal/workers/" + workerId + "/book";
        try {
            ResponseEntity<WorkerInfoDto> response = restTemplate.exchange(
                    url, HttpMethod.POST, HttpEntity.EMPTY, WorkerInfoDto.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("Tukang tidak ditemukan");
        } catch (HttpClientErrorException.Conflict e) {
            // Parse pesan error dari worker-service jika tersedia
            throw new IllegalStateException("Tukang tidak tersedia untuk dibooking");
        } catch (HttpClientErrorException e) {
            log.error("Error calling worker-service bookWorker: {} {}", e.getStatusCode(), e.getMessage());
            throw new IllegalStateException("Gagal menghubungi worker service: " + e.getMessage());
        }
    }
}
