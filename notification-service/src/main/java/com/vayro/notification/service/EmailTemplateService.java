package com.vayro.notification.service;

import com.vayro.notification.dto.BookingConfirmedNotificationRequest;
import com.vayro.notification.dto.VehicleReturnedNotificationRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class EmailTemplateService {

    private String formatInr(BigDecimal amount) {
        if (amount == null) return "0.00";
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(amount);
    }

    public String generateBookingConfirmedHtml(BookingConfirmedNotificationRequest req) {
        String customerName = req.getCustomerName() != null ? req.getCustomerName() : "Valued Customer";
        String vehicleName = req.getVehicleName() != null ? req.getVehicleName() : "Vehicle";
        String bookingRef = req.getBookingReference() != null ? req.getBookingReference() : "BK-" + req.getBookingId();
        String pickupTime = req.getPickupTime() != null ? req.getPickupTime() : "TBD";
        String returnTime = req.getReturnTime() != null ? req.getReturnTime() : "TBD";
        String pickupLocation = req.getPickupLocation() != null ? req.getPickupLocation() : "VAYRO Hub";
        String totalFormatted = formatInr(req.getTotalAmount());
        String rentalDays = req.getRentalDays() != null ? String.valueOf(req.getRentalDays()) : "1";

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Booking Confirmation - VAYRO</title>\n" +
                "</head>\n" +
                "<body style=\"margin:0; padding:0; background-color:#0f172a; font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif; color:#334155;\">\n" +
                "  <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#0f172a; padding:30px 15px;\">\n" +
                "    <tr>\n" +
                "      <td align=\"center\">\n" +
                "        <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:600px; background-color:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 10px 25px rgba(0,0,0,0.3);\">\n" +
                "          <!-- HEADER -->\n" +
                "          <tr>\n" +
                "            <td style=\"background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); padding:35px 30px; text-align:center;\">\n" +
                "              <div style=\"font-size:28px; font-weight:800; color:#ea580c; letter-spacing:2px; margin-bottom:6px;\">VAYRO</div>\n" +
                "              <div style=\"font-size:12px; font-weight:600; color:#94a3b8; letter-spacing:1px; text-transform:uppercase;\">Premium Mobility &amp; Rentals</div>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- HERO STATUS -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding:30px 30px 10px 30px; text-align:center;\">\n" +
                "              <div style=\"display:inline-block; background-color:#dcfce7; color:#15803d; font-size:13px; font-weight:700; padding:6px 16px; border-radius:20px; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:15px;\">✓ Booking Confirmed</div>\n" +
                "              <h1 style=\"margin:0 0 10px 0; font-size:24px; color:#0f172a; font-weight:700;\">You're ready to drive, " + customerName + "!</h1>\n" +
                "              <p style=\"margin:0; font-size:15px; color:#64748b; line-height:1.5;\">Your reservation for <strong>" + vehicleName + "</strong> has been confirmed and paid. Your official tax invoice is attached as a PDF.</p>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- BOOKING DETAILS CARD -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding:20px 30px;\">\n" +
                "              <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#f8fafc; border:1px solid #e2e8f0; border-radius:12px; padding:20px;\">\n" +
                "                <tr>\n" +
                "                  <td style=\"padding-bottom:12px; border-bottom:1px solid #e2e8f0;\">\n" +
                "                    <span style=\"font-size:12px; font-weight:600; color:#64748b; text-transform:uppercase;\">Booking Reference</span><br>\n" +
                "                    <strong style=\"font-size:17px; color:#ea580c; font-family:monospace;\">" + bookingRef + "</strong>\n" +
                "                  </td>\n" +
                "                  <td style=\"padding-bottom:12px; border-bottom:1px solid #e2e8f0; text-align:right;\">\n" +
                "                    <span style=\"font-size:12px; font-weight:600; color:#64748b; text-transform:uppercase;\">Total Paid</span><br>\n" +
                "                    <strong style=\"font-size:18px; color:#0f172a;\">₹ " + totalFormatted + "</strong>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <td colspan=\"2\" style=\"padding-top:14px; padding-bottom:8px;\">\n" +
                "                    <span style=\"font-size:12px; font-weight:600; color:#64748b; text-transform:uppercase;\">Vehicle</span><br>\n" +
                "                    <strong style=\"font-size:15px; color:#0f172a;\">" + vehicleName + " (" + rentalDays + " Day" + (rentalDays.equals("1") ? "" : "s") + ")</strong>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <td style=\"padding-top:8px; vertical-align:top;\">\n" +
                "                    <span style=\"font-size:12px; font-weight:600; color:#64748b; text-transform:uppercase;\">Pickup Date &amp; Time</span><br>\n" +
                "                    <span style=\"font-size:14px; color:#1e293b; font-weight:600;\">" + pickupTime + "</span><br>\n" +
                "                    <span style=\"font-size:12px; color:#64748b;\">" + pickupLocation + "</span>\n" +
                "                  </td>\n" +
                "                  <td style=\"padding-top:8px; vertical-align:top; text-align:right;\">\n" +
                "                    <span style=\"font-size:12px; font-weight:600; color:#64748b; text-transform:uppercase;\">Return Date &amp; Time</span><br>\n" +
                "                    <span style=\"font-size:14px; color:#1e293b; font-weight:600;\">" + returnTime + "</span><br>\n" +
                "                    <span style=\"font-size:12px; color:#64748b;\">" + pickupLocation + "</span>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </table>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- INVOICE NOTICE -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding:0 30px 20px 30px;\">\n" +
                "              <div style=\"background-color:#eff6ff; border-left:4px solid #3b82f6; padding:12px 16px; border-radius:0 8px 8px 0;\">\n" +
                "                <strong style=\"font-size:13px; color:#1e40af;\">📄 PDF Invoice Attached</strong>\n" +
                "                <p style=\"margin:4px 0 0 0; font-size:12px; color:#3b82f6; line-height:1.4;\">Your itemized tax invoice has been generated and attached to this email (<code>VAYRO-Invoice-" + bookingRef + ".pdf</code>). You can also view or download it anytime from your VAYRO dashboard.</p>\n" +
                "              </div>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- PICKUP CHECKLIST -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding:0 30px 25px 30px;\">\n" +
                "              <div style=\"font-size:13px; font-weight:700; color:#0f172a; margin-bottom:8px;\">Important Reminders for Pickup:</div>\n" +
                "              <ul style=\"margin:0; padding-left:18px; font-size:13px; color:#64748b; line-height:1.6;\">\n" +
                "                <li>Original Driving License &amp; Government ID required at vehicle handover.</li>\n" +
                "                <li>Please arrive at the hub 10 minutes prior to scheduled pickup.</li>\n" +
                "                <li>Vehicle comes with full fuel tank — kindly return with the same level.</li>\n" +
                "              </ul>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- FOOTER -->\n" +
                "          <tr>\n" +
                "            <td style=\"background-color:#f1f5f9; padding:20px 30px; text-align:center; border-top:1px solid #e2e8f0;\">\n" +
                "              <div style=\"font-size:12px; color:#64748b; margin-bottom:6px;\">Need assistance? Contact our 24/7 support at <strong>support@vayro.com</strong> or call <strong>+91 80000 12345</strong>.</div>\n" +
                "              <div style=\"font-size:11px; color:#94a3b8;\">© 2026 VAYRO Mobility Solutions Pvt Ltd. All rights reserved.</div>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </table>\n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </table>\n" +
                "</body>\n" +
                "</html>";
    }

    public String generateVehicleReturnedAdminHtml(VehicleReturnedNotificationRequest req) {
        String customerName = req.getCustomerName() != null ? req.getCustomerName() : "Customer";
        String vehicleName = req.getVehicleName() != null ? req.getVehicleName() : "Vehicle";
        String bookingRef = req.getBookingReference() != null ? req.getBookingReference() : "BK-" + req.getBookingId();
        String returnTimestamp = req.getReturnTimestamp() != null ? req.getReturnTimestamp() : "Just now";
        String vehicleStatus = req.getVehicleStatus() != null ? req.getVehicleStatus() : "AVAILABLE";

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Admin Notification - Vehicle Returned</title>\n" +
                "</head>\n" +
                "<body style=\"margin:0; padding:0; background-color:#0f172a; font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif; color:#334155;\">\n" +
                "  <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#0f172a; padding:30px 15px;\">\n" +
                "    <tr>\n" +
                "      <td align=\"center\">\n" +
                "        <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:600px; background-color:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 10px 25px rgba(0,0,0,0.3);\">\n" +
                "          <!-- HEADER -->\n" +
                "          <tr>\n" +
                "            <td style=\"background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); padding:30px; text-align:center;\">\n" +
                "              <div style=\"font-size:24px; font-weight:800; color:#ea580c; letter-spacing:2px; margin-bottom:4px;\">VAYRO ADMIN</div>\n" +
                "              <div style=\"font-size:12px; font-weight:600; color:#94a3b8; letter-spacing:1px; text-transform:uppercase;\">Fleet Operations Alert</div>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- STATUS ALERT -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding:25px 30px 10px 30px; text-align:center;\">\n" +
                "              <div style=\"display:inline-block; background-color:#e0f2fe; color:#0369a1; font-size:13px; font-weight:700; padding:6px 16px; border-radius:20px; text-transform:uppercase;\">🔄 Vehicle Returned &amp; Checked-In</div>\n" +
                "              <h2 style=\"margin:15px 0 5px 0; font-size:20px; color:#0f172a;\">" + vehicleName + " is Back in Fleet</h2>\n" +
                "              <p style=\"margin:0; font-size:14px; color:#64748b;\">Booking <strong>" + bookingRef + "</strong> has been marked as <strong>RETURNED</strong>.</p>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- DETAILS TABLE -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding:20px 30px 25px 30px;\">\n" +
                "              <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#f8fafc; border:1px solid #e2e8f0; border-radius:12px; padding:18px;\">\n" +
                "                <tr>\n" +
                "                  <td style=\"padding-bottom:10px; border-bottom:1px solid #e2e8f0; font-size:13px; color:#64748b;\">Booking Reference:</td>\n" +
                "                  <td style=\"padding-bottom:10px; border-bottom:1px solid #e2e8f0; text-align:right; font-size:14px; font-weight:700; color:#ea580c;\">" + bookingRef + "</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; font-size:13px; color:#64748b;\">Vehicle:</td>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; text-align:right; font-size:14px; font-weight:700; color:#0f172a;\">" + vehicleName + "</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; font-size:13px; color:#64748b;\">Customer Name:</td>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; text-align:right; font-size:14px; font-weight:600; color:#0f172a;\">" + customerName + "</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; font-size:13px; color:#64748b;\">Customer Email:</td>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; text-align:right; font-size:14px; font-weight:600; color:#0f172a;\">" + (req.getCustomerEmail() != null ? req.getCustomerEmail() : "N/A") + "</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; font-size:13px; color:#64748b;\">Customer Phone:</td>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; text-align:right; font-size:14px; font-weight:600; color:#0f172a;\">" + (req.getCustomerPhone() != null ? req.getCustomerPhone() : "N/A") + "</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; font-size:13px; color:#64748b;\">Pickup Time:</td>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; text-align:right; font-size:14px; font-weight:600; color:#0f172a;\">" + (req.getPickupTime() != null ? req.getPickupTime() : "N/A") + "</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; font-size:13px; color:#64748b;\">Scheduled Return:</td>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; text-align:right; font-size:14px; font-weight:600; color:#0f172a;\">" + (req.getReturnTime() != null ? req.getReturnTime() : "N/A") + "</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; font-size:13px; color:#64748b;\">Actual Return Time:</td>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; text-align:right; font-size:14px; font-weight:600; color:#0f172a;\">" + returnTimestamp + "</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; font-size:13px; color:#64748b;\">Booking Status:</td>\n" +
                "                  <td style=\"padding:10px 0; border-bottom:1px solid #e2e8f0; text-align:right; font-size:14px; font-weight:700; color:#0369a1;\">" + (req.getBookingStatus() != null ? req.getBookingStatus() : "RETURNED") + "</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <td style=\"padding-top:10px; font-size:13px; color:#64748b;\">Fleet Vehicle Status:</td>\n" +
                "                  <td style=\"padding-top:10px; text-align:right; font-size:14px; font-weight:700; color:#16a34a;\">" + vehicleStatus + "</td>\n" +
                "                </tr>\n" +
                "              </table>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- FOOTER -->\n" +
                "          <tr>\n" +
                "            <td style=\"background-color:#f1f5f9; padding:15px 30px; text-align:center; border-top:1px solid #e2e8f0;\">\n" +
                "              <div style=\"font-size:11px; color:#94a3b8;\">Automated System Alert • VAYRO Fleet Operations</div>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </table>\n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </table>\n" +
                "</body>\n" +
                "</html>";
    }
}
