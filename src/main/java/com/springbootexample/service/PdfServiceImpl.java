package com.springbootexample.service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.springbootexample.entity.Customer;
import com.springbootexample.entity.Transaction;
import com.springbootexample.repository.CustomerRepository;
import com.springbootexample.repository.TransactionRepository;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

@Service
public class PdfServiceImpl implements PdfService {

    @Autowired
    private CustomerRepository customerRepo;

    @Autowired
    private TransactionRepository txnRepo;

    // 🔥 ALL CUSTOMERS PDF
    @Override
    public byte[] generateCustomersPdf(Long userId) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Customers Report\n\n"));

            List<Customer> customers = customerRepo.findByUserId(userId);

            Table table = new Table(3);
            table.addCell("Name");
            table.addCell("Mobile");
            table.addCell("Balance");

            for (Customer c : customers) {
                table.addCell(c.getName() != null ? c.getName() : "-");
                table.addCell(c.getMobile() != null ? c.getMobile() : "-");
                table.addCell(String.valueOf(c.getCurrentBalance()));
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generating customers PDF");
        }

        return out.toByteArray();
    }

    // 🔥 CUSTOMER TRANSACTIONS PDF
//    @Override
//    public byte[] generateCustomerTransactionsPdf(Long customerId, Long userId) {
//
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//
//        try {
//            PdfWriter writer = new PdfWriter(out);
//            PdfDocument pdf = new PdfDocument(writer);
//            Document document = new Document(pdf);
//
//            document.add(new Paragraph("Customer Transactions\n\n"));
//
//            List<Transaction> txns = txnRepo.findBycustomerId(customerId);
//
//            // 🔥 Time formatter
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
//
//            // 🔥 Table with 4 columns
//            Table table = new Table(4);
//           // table.setWidth(100);
//            table.addCell("Type");
//            table.addCell("Amount");
//            table.addCell("Date & Time");
//            table.addCell("Description");
//
//            for (Transaction t : txns) {
//
//                if (!t.getUserId().equals(userId)) continue;
//
//                // 🔥 format time
//                String dateTime = t.getCreatedAt().format(formatter);
//
//                table.addCell(t.getType());
//                table.addCell(String.valueOf(t.getAmount()));
//                table.addCell(dateTime);
//                table.addCell(t.getDescription() != null ? t.getDescription() : "-");
//            }
//
//            document.add(table);
//            document.close();
//
//        } catch (Exception e) {
//            throw new RuntimeException("Error generating transactions PDF");
//        }
//
//        return out.toByteArray();
//    }
    @Override
    public byte[] generateCustomerTransactionsPdf(
            Long customerId,
            Long userId,
            LocalDateTime from,
            LocalDateTime to) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Customer Statement\n\n")
                    .setBold().setFontSize(16));

            List<Transaction> txns =
                    txnRepo.findByCustomerAndDateRange(customerId, from, to);

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            float[] widths = {2, 2, 3, 4};
            Table table = new Table(widths).setWidth(100);

            table.addCell("Type");
            table.addCell("Amount");
            table.addCell("Date & Time");
            table.addCell("Description");

            for (Transaction t : txns) {

                if (!t.getUserId().equals(userId)) continue;

                table.addCell(t.getType());
                table.addCell(String.valueOf(t.getAmount()));
                table.addCell(t.getCreatedAt().format(formatter));
                table.addCell(
                    t.getDescription() != null ? t.getDescription() : "-"
                );
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF");
        }

        return out.toByteArray();
    }

	

}