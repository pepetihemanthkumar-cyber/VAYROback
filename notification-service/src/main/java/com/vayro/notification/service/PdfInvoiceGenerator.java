package com.vayro.notification.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.vayro.notification.dto.BookingConfirmedNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfInvoiceGenerator {

    private static final Logger log = LoggerFactory.getLogger(PdfInvoiceGenerator.class);

    // Primary Brand Colors
    private static final Color COLOR_PRIMARY = new Color(15, 23, 42);      // Slate 900
    private static final Color COLOR_ACCENT = new Color(234, 88, 12);      // Orange 600 (VAYRO Brand)
    private static final Color COLOR_TEXT_DARK = new Color(30, 41, 59);    // Slate 800
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);// Slate 500
    private static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);  // Slate 50
    private static final Color COLOR_BORDER = new Color(226, 232, 240);    // Slate 200
    private static final Color COLOR_SUCCESS = new Color(22, 163, 74);     // Green 600

    public byte[] generateBookingInvoice(BookingConfirmedNotificationRequest req) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPdfVersion(PdfWriter.VERSION_1_7);

            document.open();

            // Fonts
            Font fontBrand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, COLOR_ACCENT);
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_PRIMARY);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_TEXT_DARK);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_TEXT_DARK);
            Font fontMuted = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_MUTED);
            Font fontMutedBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_TEXT_MUTED);
            Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_PRIMARY);
            Font fontPaidBadge = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_SUCCESS);

            // 1. Header Table (Brand & Invoice Meta)
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1.2f, 1f});

            // Brand cell (Left)
            PdfPCell brandCell = new PdfPCell();
            brandCell.setBorder(Rectangle.NO_BORDER);
            brandCell.addElement(new Paragraph("VAYRO", fontBrand));
            Paragraph tagline = new Paragraph("PREMIUM VEHICLE RENTALS & MOBILITY", fontMutedBold);
            tagline.setSpacingAfter(4f);
            brandCell.addElement(tagline);
            brandCell.addElement(new Paragraph("support@vayro.com | +91 80000 12345 | www.vayro.com", fontMuted));
            headerTable.addCell(brandCell);

            // Invoice Details cell (Right)
            PdfPCell invoiceMetaCell = new PdfPCell();
            invoiceMetaCell.setBorder(Rectangle.NO_BORDER);
            invoiceMetaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph invTitle = new Paragraph("TAX INVOICE / RECEIPT", fontTitle);
            invTitle.setAlignment(Element.ALIGN_RIGHT);
            invoiceMetaCell.addElement(invTitle);

            String invNo = "INV-" + (req.getBookingReference() != null ? req.getBookingReference() : "VAYRO-" + req.getBookingId());
            Paragraph invNumP = new Paragraph("Invoice No: " + invNo, fontHeader);
            invNumP.setAlignment(Element.ALIGN_RIGHT);
            invoiceMetaCell.addElement(invNumP);

            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
            Paragraph invDateP = new Paragraph("Date: " + dateStr, fontMuted);
            invDateP.setAlignment(Element.ALIGN_RIGHT);
            invoiceMetaCell.addElement(invDateP);

            Paragraph paidP = new Paragraph("STATUS: PAID (SIMULATED)", fontPaidBadge);
            paidP.setAlignment(Element.ALIGN_RIGHT);
            invoiceMetaCell.addElement(paidP);

            headerTable.addCell(invoiceMetaCell);
            document.add(headerTable);

            // Divider
            document.add(new Paragraph(" "));
            LineSeparator separator = new LineSeparator(1f, 100f, COLOR_BORDER, Element.ALIGN_CENTER, -2f);
            document.add(new Chunk(separator));
            document.add(new Paragraph(" "));

            // 2. Customer & Rental Information Table
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1f, 1f});
            infoTable.setSpacingAfter(15f);

            // Billed To
            PdfPCell billedToCell = new PdfPCell();
            billedToCell.setBorder(Rectangle.BOX);
            billedToCell.setBorderColor(COLOR_BORDER);
            billedToCell.setBackgroundColor(COLOR_BG_LIGHT);
            billedToCell.setPadding(10f);

            billedToCell.addElement(new Paragraph("BILLED TO", fontMutedBold));
            billedToCell.addElement(new Paragraph(req.getCustomerName() != null ? req.getCustomerName() : "Valued Customer", fontHeader));
            if (req.getCustomerEmail() != null) {
                billedToCell.addElement(new Paragraph("Email: " + req.getCustomerEmail(), fontBody));
            }
            if (req.getCustomerPhone() != null) {
                billedToCell.addElement(new Paragraph("Phone: " + req.getCustomerPhone(), fontBody));
            }
            billedToCell.addElement(new Paragraph("Booking Ref: " + (req.getBookingReference() != null ? req.getBookingReference() : String.valueOf(req.getBookingId())), fontBody));
            infoTable.addCell(billedToCell);

            // Rental Schedule & Vehicle
            PdfPCell rentalCell = new PdfPCell();
            rentalCell.setBorder(Rectangle.BOX);
            rentalCell.setBorderColor(COLOR_BORDER);
            rentalCell.setBackgroundColor(COLOR_BG_LIGHT);
            rentalCell.setPadding(10f);

            rentalCell.addElement(new Paragraph("RENTAL DETAILS", fontMutedBold));
            rentalCell.addElement(new Paragraph("Vehicle: " + (req.getVehicleName() != null ? req.getVehicleName() : "Vehicle"), fontHeader));
            if (req.getPickupLocation() != null) {
                rentalCell.addElement(new Paragraph("Location: " + req.getPickupLocation(), fontBody));
            }
            rentalCell.addElement(new Paragraph("Pickup: " + req.getPickupTime(), fontBody));
            rentalCell.addElement(new Paragraph("Return: " + req.getReturnTime(), fontBody));
            infoTable.addCell(rentalCell);

            document.add(infoTable);

            // 3. Itemized Charges Table
            PdfPTable itemsTable = new PdfPTable(3);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{3f, 1f, 1.2f});
            itemsTable.setSpacingBefore(5f);
            itemsTable.setSpacingAfter(15f);

            // Table Header
            addTableHeaderCell(itemsTable, "ITEM / DESCRIPTION", fontMutedBold, Element.ALIGN_LEFT);
            addTableHeaderCell(itemsTable, "QTY / DURATION", fontMutedBold, Element.ALIGN_CENTER);
            addTableHeaderCell(itemsTable, "AMOUNT (INR)", fontMutedBold, Element.ALIGN_RIGHT);

            // Number formatter
            NumberFormat inrFormat = NumberFormat.getNumberInstance(new Locale("en", "IN"));
            inrFormat.setMinimumFractionDigits(2);
            inrFormat.setMaximumFractionDigits(2);

            // Vehicle rental item
            String vehicleDesc = "Base Rental - " + (req.getVehicleName() != null ? req.getVehicleName() : "Vehicle");
            String duration = (req.getRentalDays() != null && req.getRentalDays() > 0) ? req.getRentalDays() + " Day(s)" : "1 Booking";
            
            BigDecimal totalAmt = req.getTotalAmount() != null ? req.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal baseRental = req.getBaseAmount() != null ? req.getBaseAmount() : totalAmt;

            addTableRowCell(itemsTable, vehicleDesc, fontBody, Element.ALIGN_LEFT);
            addTableRowCell(itemsTable, duration, fontBody, Element.ALIGN_CENTER);
            addTableRowCell(itemsTable, "₹ " + inrFormat.format(baseRental), fontBody, Element.ALIGN_RIGHT);

            // Add-ons row if any
            if (req.getAddOnsAmount() != null && req.getAddOnsAmount().compareTo(BigDecimal.ZERO) > 0) {
                addTableRowCell(itemsTable, "Rental Add-ons & Equipment", fontBody, Element.ALIGN_LEFT);
                addTableRowCell(itemsTable, "Included", fontBody, Element.ALIGN_CENTER);
                addTableRowCell(itemsTable, "₹ " + inrFormat.format(req.getAddOnsAmount()), fontBody, Element.ALIGN_RIGHT);
            }

            // Discount row if any
            if (req.getDiscountAmount() != null && req.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                addTableRowCell(itemsTable, "Promotional Discount", fontBody, Element.ALIGN_LEFT);
                addTableRowCell(itemsTable, "1", fontBody, Element.ALIGN_CENTER);
                addTableRowCell(itemsTable, "- ₹ " + inrFormat.format(req.getDiscountAmount()), fontBody, Element.ALIGN_RIGHT);
            }

            // Tax / GST row
            if (req.getTaxAmount() != null && req.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                addTableRowCell(itemsTable, "GST @ 18.0% (Academic / Demo Calculation)", fontBody, Element.ALIGN_LEFT);
                addTableRowCell(itemsTable, "18%", fontBody, Element.ALIGN_CENTER);
                addTableRowCell(itemsTable, "₹ " + inrFormat.format(req.getTaxAmount()), fontBody, Element.ALIGN_RIGHT);
            }

            // Security Deposit row if any
            if (req.getSecurityDeposit() != null && req.getSecurityDeposit().compareTo(BigDecimal.ZERO) > 0) {
                addTableRowCell(itemsTable, "Refundable Security Deposit", fontBody, Element.ALIGN_LEFT);
                addTableRowCell(itemsTable, "1", fontBody, Element.ALIGN_CENTER);
                addTableRowCell(itemsTable, "₹ " + inrFormat.format(req.getSecurityDeposit()), fontBody, Element.ALIGN_RIGHT);
            }

            // Subtotal / Total rows
            addSummaryRow(itemsTable, "Total Amount Paid:", "₹ " + inrFormat.format(totalAmt), fontTotal);

            document.add(itemsTable);

            // 4. Payment Information & Notes
            PdfPTable footerTable = new PdfPTable(1);
            footerTable.setWidthPercentage(100);

            PdfPCell footerCell = new PdfPCell();
            footerCell.setBorder(Rectangle.BOX);
            footerCell.setBorderColor(COLOR_BORDER);
            footerCell.setBackgroundColor(COLOR_BG_LIGHT);
            footerCell.setPadding(10f);

            footerCell.addElement(new Paragraph("PAYMENT & RENTAL TERMS", fontMutedBold));
            footerCell.addElement(new Paragraph("• Payment Method: " + req.getPaymentMethod(), fontMuted));
            footerCell.addElement(new Paragraph("• Payment Reference: " + req.getPaymentReference(), fontMuted));
            footerCell.addElement(new Paragraph("• Fuel Policy: Return vehicle with identical fuel level as provided at pickup.", fontMuted));
            footerCell.addElement(new Paragraph("• Identification: Please carry your valid Driving License & Government ID at pickup time.", fontMuted));
            footerCell.addElement(new Paragraph("• Support: For 24/7 roadside assistance, call +91 80000 12345.", fontMuted));
            footerCell.addElement(new Paragraph(" ", fontMuted));
            footerCell.addElement(new Paragraph("Note: This is an academic project computer-generated invoice and requires no physical signature.", fontMutedBold));

            footerTable.addCell(footerCell);
            document.add(footerTable);

            document.close();
            byte[] pdfBytes = out.toByteArray();
            log.info("[PDF_INVOICE_GENERATED] Generated PDF invoice for booking reference '{}' ({} bytes)", req.getBookingReference(), pdfBytes.length);
            return pdfBytes;

        } catch (Exception e) {
            log.error("[PDF_INVOICE_FAILED] Failed to generate PDF invoice: {}", e.getMessage(), e);
            throw new RuntimeException("PDF Invoice generation failed: " + e.getMessage(), e);
        }
    }

    private void addTableHeaderCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(COLOR_BG_LIGHT);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(1.5f);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private void addTableRowCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setColspan(2);
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(8f);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valCell = new PdfPCell(new Phrase(value, font));
        valCell.setBorder(Rectangle.NO_BORDER);
        valCell.setPadding(8f);
        valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valCell);
    }
}
