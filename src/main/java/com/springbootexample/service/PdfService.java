package com.springbootexample.service;

import java.time.LocalDateTime;

public interface PdfService {

	    byte[] generateCustomersPdf(Long userId);

	    //byte[] generateCustomerTransactionsPdf(Long customerId, Long userId);
	    byte[] generateCustomerTransactionsPdf(
	            Long customerId,
	            Long userId,
	            LocalDateTime from,
	            LocalDateTime to
	    );
	}
