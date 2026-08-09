package com.roamate.finance.dto;

import com.roamate.finance.domain.PaymentSource;

public record PaymentLineDto(PaymentSource source, String payerUserId, long amountCents) {}
