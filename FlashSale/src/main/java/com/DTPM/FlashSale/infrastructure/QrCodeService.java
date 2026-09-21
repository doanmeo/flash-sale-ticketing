package com.DTPM.FlashSale.infrastructure;

import org.springframework.stereotype.Service;

@Service
public class QrCodeService {

    public String generate(String orderCode) {
        return "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=PAY-" + orderCode;
    }
}
