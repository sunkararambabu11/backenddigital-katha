package com.springbootexample.controller;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.springbootexample.dto.TransactionPdfRequest;
import com.springbootexample.security.JwtUtil;
import com.springbootexample.service.PdfService;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = header.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }

    // ALL CUSTOMERS PDF
    @GetMapping("/customers")
    public ResponseEntity<byte[]> downloadCustomers(HttpServletRequest request) {

        Long userId = getUserId(request);

        byte[] pdf = pdfService.generateCustomersPdf(userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customers.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // CUSTOMER TRANSACTIONS PDF
    @PostMapping("/transactions")
    public ResponseEntity<byte[]> downloadTransactions(
            @RequestBody TransactionPdfRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getUserId(httpRequest);

        LocalDateTime from =
                LocalDate.parse(request.getFromDate()).atStartOfDay();

        LocalDateTime to =
                LocalDate.parse(request.getToDate()).atTime(23, 59, 59);

        byte[] pdf = pdfService.generateCustomerTransactionsPdf(
                request.getCustomerId(),
                userId,
                from,
                to
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=statement.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
