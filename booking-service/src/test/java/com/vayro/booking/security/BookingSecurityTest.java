package com.vayro.booking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vayro.booking.client.VehicleServiceClient;
import com.vayro.booking.dto.BookingCancelRequest;
import com.vayro.booking.dto.BookingCreateRequest;
import com.vayro.booking.dto.BookingResponse;
import com.vayro.booking.dto.PageResponse;
import com.vayro.booking.entity.BookingStatus;
import com.vayro.booking.entity.PaymentStatus;
import com.vayro.booking.exception.BookingNotFoundException;
import com.vayro.booking.exception.BookingOverlapException;
import com.vayro.booking.exception.InvalidBookingDateException;
import com.vayro.booking.exception.InvalidBookingStateException;
import com.vayro.booking.exception.UnauthorizedBookingAccessException;
import com.vayro.booking.service.BookingService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private VehicleServiceClient vehicleServiceClient;

    @Value("${jwt.secret}")
    private String secretKey;

    private ObjectMapper objectMapper;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        userToken = generateToken("1001", "customer@vayro.com", "USER", "Alice Customer", 3600000);
        adminToken = generateToken("9999", "admin@vayro.com", "ADMIN", "Vayro Admin", 3600000);
    }

    private String generateToken(String userId, String email, String role, String name, long expirationMs) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
        } catch (Exception e) {
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("name", name);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    private BookingResponse createSampleResponse(Long id, String ref, BookingStatus status) {
        BookingResponse r = new BookingResponse();
        r.setId(id);
        r.setBookingReference(ref);
        r.setVehicleId("veh-001");
        r.setVehicleName("BMW 3 Series");
        r.setUserId("1001");
        r.setCustomerName("Alice Customer");
        r.setCustomerEmail("customer@vayro.com");
        r.setPickupDateTime(LocalDateTime.now().plusDays(1));
        r.setReturnDateTime(LocalDateTime.now().plusDays(4));
        r.setRentalDays(3);
        r.setPricePerDay(BigDecimal.valueOf(4500));
        r.setBaseAmount(BigDecimal.valueOf(13500));
        r.setAddOnsAmount(BigDecimal.ZERO);
        r.setDiscountAmount(BigDecimal.ZERO);
        r.setTaxAmount(BigDecimal.valueOf(2430));
        r.setTotalAmount(BigDecimal.valueOf(15930));
        r.setStatus(status);
        r.setPaymentStatus(PaymentStatus.PENDING);
        r.setPickupLocation("Vijayawada Airport");
        r.setReturnLocation("Vijayawada Airport");
        return r;
    }

    @Test
    @DisplayName("Security: Valid JWT authentication allows booking creation (201 Created)")
    void testCreateBookingWithValidJwt() throws Exception {
        BookingCreateRequest req = new BookingCreateRequest(
                "veh-001",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(4),
                "Vijayawada Airport",
                "Vijayawada Airport"
        );

        BookingResponse mockRes = createSampleResponse(1L, "BK-TEST-1234", BookingStatus.PENDING);
        when(bookingService.createBooking(any(BookingCreateRequest.class), any(AuthenticatedUser.class), any()))
                .thenReturn(mockRes);

        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.bookingReference").value("BK-TEST-1234"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Security: Missing JWT token returns 401 Unauthorized")
    void testMissingJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/bookings/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Security: Invalid / Expired JWT is rejected with 401 Unauthorized")
    void testInvalidJwtReturns401() throws Exception {
        String expiredToken = generateToken("1001", "customer@vayro.com", "USER", "Alice Customer", -5000);

        mockMvc.perform(get("/api/bookings/my")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Security: USER role cannot access ADMIN booking list (403 Forbidden)")
    void testUserCannotAccessAdminBookingList() throws Exception {
        mockMvc.perform(get("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: ADMIN role can access all bookings endpoint (200 OK)")
    void testAdminCanAccessAllBookings() throws Exception {
        BookingResponse r = createSampleResponse(1L, "BK-ADMIN-1", BookingStatus.CONFIRMED);
        PageResponse<BookingResponse> page = new PageResponse<>(List.of(r), 0, 20, 1, 1, true);

        when(bookingService.getAllBookings(any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].bookingReference").value("BK-ADMIN-1"));
    }

    @Test
    @DisplayName("Security: USER accessing another user's booking receives 403 Forbidden")
    void testUserAccessingAnotherUsersBookingReturns403() throws Exception {
        when(bookingService.getBookingById(eq(999L), any()))
                .thenThrow(new UnauthorizedBookingAccessException("You do not have permission to view or manage this booking."));

        mockMvc.perform(get("/api/bookings/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Exceptions: Booking not found returns 404 Not Found")
    void testBookingNotFoundReturns404() throws Exception {
        when(bookingService.getBookingById(eq(404L), any()))
                .thenThrow(new BookingNotFoundException("Booking not found with ID: 404"));

        mockMvc.perform(get("/api/bookings/404")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("BOOKING_NOT_FOUND"));
    }

    @Test
    @DisplayName("Exceptions: Invalid state transition returns 400 Bad Request")
    void testInvalidStateTransitionReturns400() throws Exception {
        when(bookingService.confirmBooking(eq(1L), any(), any()))
                .thenThrow(new InvalidBookingStateException("Cannot confirm booking with status COMPLETED."));

        mockMvc.perform(patch("/api/bookings/1/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_BOOKING_STATE"));
    }

    @Test
    @DisplayName("Exceptions: Overlap conflict in booking creation returns 409 Conflict")
    void testOverlapConflictReturns409() throws Exception {
        BookingCreateRequest req = new BookingCreateRequest(
                "veh-001",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(4),
                "HQ", "HQ"
        );

        when(bookingService.createBooking(any(), any(), any()))
                .thenThrow(new BookingOverlapException("Vehicle 'BMW 3 Series' is already booked for the selected period."));

        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("BOOKING_CONFLICT"));
    }

    @Test
    @DisplayName("Admin: Lifecycle endpoints confirm, activate, return, complete succeed for ADMIN")
    void testAdminLifecycleEndpoints() throws Exception {
        BookingResponse confirmed = createSampleResponse(1L, "BK-1", BookingStatus.CONFIRMED);
        BookingResponse active = createSampleResponse(1L, "BK-1", BookingStatus.ACTIVE);
        BookingResponse returned = createSampleResponse(1L, "BK-1", BookingStatus.RETURNED);
        BookingResponse completed = createSampleResponse(1L, "BK-1", BookingStatus.COMPLETED);

        when(bookingService.confirmBooking(eq(1L), any(), any())).thenReturn(confirmed);
        when(bookingService.activateBooking(eq(1L), any(), any())).thenReturn(active);
        when(bookingService.returnBooking(eq(1L), any(), any())).thenReturn(returned);
        when(bookingService.completeBooking(eq(1L), any(), any())).thenReturn(completed);

        // Confirm
        mockMvc.perform(patch("/api/bookings/1/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Activate
        mockMvc.perform(patch("/api/bookings/1/activate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Return
        mockMvc.perform(patch("/api/bookings/1/return")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        // Complete
        mockMvc.perform(patch("/api/bookings/1/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Cancellation: Cancel booking returns 200 on valid cancellation")
    void testCancelBookingEndpoint() throws Exception {
        BookingResponse cancelled = createSampleResponse(1L, "BK-1", BookingStatus.CANCELLED);
        when(bookingService.cancelBooking(eq(1L), any(), any(), any())).thenReturn(cancelled);

        mockMvc.perform(patch("/api/bookings/1/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingCancelRequest("Schedule change"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
