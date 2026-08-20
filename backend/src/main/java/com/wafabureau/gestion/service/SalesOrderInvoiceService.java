package com.wafabureau.gestion.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wafabureau.gestion.enums.SalesOrderStatus;
import com.wafabureau.gestion.exception.BusinessException;
import com.wafabureau.gestion.exception.ResourceNotFoundException;
import com.wafabureau.gestion.model.Customer;
import com.wafabureau.gestion.model.SalesOrder;
import com.wafabureau.gestion.model.SalesOrderItem;
import com.wafabureau.gestion.repository.SalesOrderRepository;

@Service
public class SalesOrderInvoiceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Casablanca");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);

    private final SalesOrderRepository salesOrderRepository;

    public SalesOrderInvoiceService(SalesOrderRepository salesOrderRepository) {
        this.salesOrderRepository = salesOrderRepository;
    }

    @Transactional(readOnly = true)
    public InvoicePdf generate(Long salesOrderId) {
        SalesOrder order = salesOrderRepository.findDetailedById(salesOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order", salesOrderId));
        if (order.getStatus() != SalesOrderStatus.CONFIRMED
                && order.getStatus() != SalesOrderStatus.DELIVERED) {
            throw new BusinessException(
                    "INVOICE_NOT_AVAILABLE",
                    "An invoice can only be generated for a confirmed or delivered sales order.");
        }

        String reference = "INV-" + order.getOrderNumber();
        try {
            return new InvoicePdf(reference + ".pdf", render(order, reference));
        } catch (IOException exception) {
            throw new IllegalStateException("The invoice PDF could not be generated.", exception);
        }
    }

    private byte[] render(SalesOrder order, String reference) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDDocumentInformation information = document.getDocumentInformation();
            information.setTitle(reference);
            information.setAuthor("WAFA BUREAU");
            String issueDate = DATE_FORMAT.format(LocalDate.ofInstant(order.getConfirmedAt(), BUSINESS_ZONE));

            PageWriter page = newPage(document, reference, issueDate, false);
            page.y = writeCustomer(page.content, order.getCustomer(), page.y);
            page.y = writeTableHeader(page.content, page.y);

            for (SalesOrderItem item : order.getItems()) {
                if (page.y < 145) {
                    page.close();
                    page = newPage(document, reference, issueDate, true);
                    page.y = writeTableHeader(page.content, page.y);
                }
                writeItem(page.content, item, page.y);
                page.y -= 28;
            }

            if (page.y < 185) {
                page.close();
                page = newPage(document, reference, issueDate, true);
            }
            writeTotals(page.content, order, page.y - 10);
            page.close();

            document.save(output);
            return output.toByteArray();
        }
    }

    private PageWriter newPage(PDDocument document, String reference, String issueDate, boolean continuation)
            throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream content = new PDPageContentStream(document, page);
        float y = 792;
        text(content, BOLD, 20, MARGIN, y, "WAFA BUREAU");
        text(content, REGULAR, 10, MARGIN, y - 20, "Internal business invoice");
        textRight(content, BOLD, 12, PAGE_WIDTH - MARGIN, y, continuation ? "INVOICE (continued)" : "INVOICE");
        textRight(content, REGULAR, 10, PAGE_WIDTH - MARGIN, y - 20, "Reference: " + reference);
        if (!continuation) {
            textRight(content, REGULAR, 10, PAGE_WIDTH - MARGIN, y - 36, "Issue date: " + issueDate);
        }
        line(content, MARGIN, y - 54, PAGE_WIDTH - MARGIN, y - 54);
        return new PageWriter(content, y - 82);
    }

    private float writeCustomer(PDPageContentStream content, Customer customer, float y) throws IOException {
        text(content, BOLD, 11, MARGIN, y, "Customer");
        y -= 18;
        text(content, BOLD, 10, MARGIN, y, customer.getName());
        if (customer.getIce() != null) {
            y -= 16;
            text(content, REGULAR, 10, MARGIN, y, "ICE: " + customer.getIce());
        }
        if (customer.getAddress() != null) {
            y -= 16;
            text(content, REGULAR, 10, MARGIN, y, "Address: " + fit(customer.getAddress(), 82));
        }
        return y - 30;
    }

    private float writeTableHeader(PDPageContentStream content, float y) throws IOException {
        content.setNonStrokingColor(0.945f, 0.961f, 0.976f);
        content.addRect(MARGIN, y - 18, CONTENT_WIDTH, 24);
        content.fill();
        content.setNonStrokingColor(0.059f, 0.090f, 0.165f);
        text(content, BOLD, 9, MARGIN + 6, y - 10, "Product");
        textRight(content, BOLD, 9, 330, y - 10, "Quantity");
        textRight(content, BOLD, 9, 435, y - 10, "Unit price");
        textRight(content, BOLD, 9, PAGE_WIDTH - MARGIN - 6, y - 10, "Line total");
        return y - 30;
    }

    private void writeItem(PDPageContentStream content, SalesOrderItem item, float y) throws IOException {
        String product = item.getProduct().getSku() + " - " + item.getProduct().getName();
        text(content, REGULAR, 9, MARGIN + 6, y, fit(product, 43));
        textRight(content, REGULAR, 9, 330, y, item.getQuantity().toString());
        textRight(content, REGULAR, 9, 435, y, money(item.getUnitPrice()));
        textRight(content, REGULAR, 9, PAGE_WIDTH - MARGIN - 6, y, money(item.getLineTotal()));
        line(content, MARGIN, y - 10, PAGE_WIDTH - MARGIN, y - 10);
    }

    private void writeTotals(PDPageContentStream content, SalesOrder order, float y) throws IOException {
        float labelX = 365;
        float valueX = PAGE_WIDTH - MARGIN - 6;
        totalRow(content, REGULAR, 10, labelX, valueX, y, "Subtotal", order.getSubtotal());
        y -= 20;
        totalRow(content, REGULAR, 10, labelX, valueX, y, "Discount", order.getDiscountAmount());
        if (order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            y -= 20;
            totalRow(content, REGULAR, 10, labelX, valueX, y, "Tax", order.getTaxAmount());
        }
        y -= 8;
        line(content, labelX, y, valueX, y);
        y -= 22;
        totalRow(content, BOLD, 12, labelX, valueX, y, "Total", order.getTotalAmount());
        textRight(content, REGULAR, 9, valueX, y - 18, "Currency: MAD");
    }

    private void totalRow(PDPageContentStream content, PDFont font, float size, float labelX, float valueX,
                          float y, String label, BigDecimal value) throws IOException {
        text(content, font, size, labelX, y, label);
        textRight(content, font, size, valueX, y, money(value));
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " MAD";
    }

    private static String fit(String value, int maxLength) {
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength - 1) + "…";
    }

    private static void text(PDPageContentStream content, PDFont font, float size, float x, float y, String value)
            throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safeText(value));
        content.endText();
    }

    private static void textRight(PDPageContentStream content, PDFont font, float size, float right, float y,
                                  String value) throws IOException {
        String safeValue = safeText(value);
        float width = font.getStringWidth(safeValue) / 1000 * size;
        text(content, font, size, right - width, y, safeValue);
    }

    private static String safeText(String value) throws IOException {
        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            try {
                REGULAR.encode(character);
                result.append(character);
            } catch (IllegalArgumentException exception) {
                result.append('?');
            }
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }

    private static void line(PDPageContentStream content, float fromX, float fromY, float toX, float toY)
            throws IOException {
        content.setStrokingColor(0.796f, 0.835f, 0.882f);
        content.setLineWidth(0.5f);
        content.moveTo(fromX, fromY);
        content.lineTo(toX, toY);
        content.stroke();
    }

    public record InvoicePdf(String filename, byte[] content) {
    }

    private static final class PageWriter {
        private final PDPageContentStream content;
        private float y;

        private PageWriter(PDPageContentStream content, float y) {
            this.content = content;
            this.y = y;
        }

        private void close() throws IOException {
            content.close();
        }
    }
}
