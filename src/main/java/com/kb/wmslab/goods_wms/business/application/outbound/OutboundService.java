package com.kb.wmslab.goods_wms.business.application.outbound;

import com.kb.wmslab.goods_wms.business.domain.common.exception.EntityNotFoundException;
import com.kb.wmslab.goods_wms.business.domain.inventory.Inventory;
import com.kb.wmslab.goods_wms.business.domain.inventory.InventoryRepository;
import com.kb.wmslab.goods_wms.business.domain.member.Member;
import com.kb.wmslab.goods_wms.business.domain.member.MemberRepository;
import com.kb.wmslab.goods_wms.business.domain.outbound.Outbound;
import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundLine;
import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundRepository;
import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundStatus;
import com.kb.wmslab.goods_wms.business.domain.product.ProductRepository;
import com.kb.wmslab.goods_wms.business.domain.warehouse.Warehouse;
import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseRepository;
import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseZone;
import com.kb.wmslab.goods_wms.business.domain.warehouse.ZoneType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboundService implements OutboundUseCase {

    private final OutboundRepository outboundRepository;
    private final WarehouseRepository warehouseRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public OutboundResult createOutbound(OutboundCommand.CreateOutbound command) {
        Warehouse warehouse = findWarehouseById(command.warehouseId());
        warehouse.validateAvailableForTransaction();

        Member handler = findMemberById(command.handlerId());
        handler.validateCanHandleOutbound();

        Outbound outbound = Outbound.create(command.warehouseId(), command.handlerId(), command.destination(), command.purpose());
        return OutboundResult.from(outboundRepository.save(outbound));
    }

    @Override
    @Transactional
    public OutboundResult addLine(Long outboundId, OutboundCommand.AddLine command) {
        Outbound outbound = findOutboundById(outboundId);
        productRepository.findById(command.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product", command.productId()));

        OutboundLine line = OutboundLine.create(command.productId(), command.quantity());
        outbound.addLine(line);
        return OutboundResult.from(outboundRepository.save(outbound));
    }

    @Override
    @Transactional
    public OutboundResult validateOutbound(Long outboundId) {
        Outbound outbound = findOutboundById(outboundId);

        Long normalZoneId = findNormalZoneId(outbound.getWarehouseId());

        for (OutboundLine line : outbound.getLines()) {
            Inventory inventory = inventoryRepository
                    .findByWarehouseIdAndZoneIdAndProductId(outbound.getWarehouseId(), normalZoneId, line.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Inventory", line.getProductId()));
            inventory.reserve(line.getQuantity());
            inventoryRepository.save(inventory);
        }

        outbound.validate();
        return OutboundResult.from(outboundRepository.save(outbound));
    }

    @Override
    @Transactional
    public OutboundResult completeOutbound(Long outboundId) {
        Outbound outbound = findOutboundById(outboundId);

        Long normalZoneId = findNormalZoneId(outbound.getWarehouseId());

        for (OutboundLine line : outbound.getLines()) {
            Inventory inventory = inventoryRepository
                    .findByWarehouseIdAndZoneIdAndProductId(outbound.getWarehouseId(), normalZoneId, line.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Inventory", line.getProductId()));
            inventory.releaseReservation(line.getQuantity());
            inventory.decreaseNormal(line.getQuantity());
            inventoryRepository.save(inventory);
        }

        outbound.complete();
        return OutboundResult.from(outboundRepository.save(outbound));
    }

    @Override
    @Transactional
    public OutboundResult cancelOutbound(Long outboundId) {
        Outbound outbound = findOutboundById(outboundId);

        if (outbound.getStatus() == OutboundStatus.VALIDATED) {
            Long normalZoneId = findNormalZoneId(outbound.getWarehouseId());
            for (OutboundLine line : outbound.getLines()) {
                inventoryRepository
                        .findByWarehouseIdAndZoneIdAndProductId(outbound.getWarehouseId(), normalZoneId, line.getProductId())
                        .ifPresent(inv -> {
                            inv.releaseReservation(line.getQuantity());
                            inventoryRepository.save(inv);
                        });
            }
        }

        outbound.cancel();
        return OutboundResult.from(outboundRepository.save(outbound));
    }

    @Override
    public OutboundResult getOutbound(Long id) {
        return OutboundResult.from(findOutboundById(id));
    }

    private Long findNormalZoneId(Long warehouseId) {
        Warehouse warehouse = findWarehouseById(warehouseId);
        return warehouse.getZones().stream()
                .filter(z -> z.getZoneType() == ZoneType.NORMAL)
                .findFirst()
                .map(WarehouseZone::getId)
                .orElseThrow(() -> new IllegalStateException("NORMAL 구역이 없습니다. warehouseId=" + warehouseId));
    }

    private Outbound findOutboundById(Long id) {
        return outboundRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outbound", id));
    }

    private Warehouse findWarehouseById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse", id));
    }

    private Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member", id));
    }
}
