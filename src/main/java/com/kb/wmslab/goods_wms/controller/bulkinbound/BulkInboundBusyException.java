package com.kb.wmslab.goods_wms.controller.bulkinbound;

public class BulkInboundBusyException extends RuntimeException {

    public BulkInboundBusyException() {
        super("대량 입고 처리 중입니다.");
    }
}
