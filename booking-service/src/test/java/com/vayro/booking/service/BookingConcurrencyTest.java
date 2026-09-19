package com.vayro.booking.service;

import com.vayro.booking.client.VehicleServiceClient;
import com.vayro.booking.dto.BookingCreateRequest;
import com.vayro.booking.dto.BookingResponse;
import com.vayro.booking.dto.VehicleDto;
import com.vayro.booking.exception.BookingOverlapException;
import com.vayro.booking.repository.BookingRepository;
import com.vayro.booking.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @MockitoBean
    private VehicleServiceClient vehicleServiceClient;

    @Test
    @DisplayName("Concurrency: 10 simultaneous booking requests for the same vehicle and period result in exactly ONE success and 9 conflict rejections")
    void testSimultaneous10BookingsPreventDoubleBooking() throws InterruptedException {
        bookingRepository.deleteAll();

        VehicleDto vehicle = new VehicleDto();
        vehicle.setId("aprilia-rs-457");
        vehicle.setName("Aprilia RS 457");
        vehicle.setPricePerDay(2400.00);
        vehicle.setStatus("AVAILABLE");

        when(vehicleServiceClient.getVehicleById("aprilia-rs-457")).thenReturn(Optional.of(vehicle));

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger overlapRejectCount = new AtomicInteger(0);

        LocalDateTime pickup = LocalDateTime.of(2026, 11, 1, 10, 0);
        LocalDateTime dropoff = LocalDateTime.of(2026, 11, 5, 10, 0);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all 10 threads to fire simultaneously
                    AuthenticatedUser user = new AuthenticatedUser(
                            "user-" + threadId,
                            "user" + threadId + "@vayro.com",
                            "USER",
                            "Customer " + threadId
                    );
                    BookingCreateRequest request = new BookingCreateRequest(
                            "aprilia-rs-457",
                            pickup,
                            dropoff,
                            "VAYRO Vijayawada",
                            "VAYRO Vijayawada"
                    );
                    bookingService.createBooking(request, user, "token");
                    successCount.incrementAndGet();
                } catch (BookingOverlapException e) {
                    overlapRejectCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected error
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all 10 threads concurrently
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly 1 must succeed, and 9 must be rejected due to overlap
        assertEquals(1, successCount.get(), "Exactly one concurrent booking must succeed");
        assertEquals(9, overlapRejectCount.get(), "9 concurrent overlapping requests must be rejected with BookingOverlapException");
        assertEquals(1, bookingRepository.count(), "Repository must contain exactly one booking record");
    }
}
