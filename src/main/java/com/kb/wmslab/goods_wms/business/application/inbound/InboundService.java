package com.kb.wmslab.goods_wms.business.application.inbound;

import com.kb.wmslab.goods_wms.business.domain.common.exception.EntityNotFoundException;
import com.kb.wmslab.goods_wms.business.domain.inbound.Inbound;
import com.kb.wmslab.goods_wms.business.domain.inbound.InboundLine;
import com.kb.wmslab.goods_wms.business.domain.inbound.InboundRepository;
import com.kb.wmslab.goods_wms.business.domain.inventory.Inventory;
import com.kb.wmslab.goods_wms.business.domain.inventory.InventoryRepository;
import com.kb.wmslab.goods_wms.business.domain.member.Member;
import com.kb.wmslab.goods_wms.business.domain.member.MemberRepository;
import com.kb.wmslab.goods_wms.business.domain.product.ProductRepository;
import com.kb.wmslab.goods_wms.business.domain.warehouse.Warehouse;
import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseRepository;
import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseZone;
import com.kb.wmslab.goods_wms.business.domain.warehouse.ZoneType;
import com.kb.wmslab.goods_wms.config.InventoryCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InboundService implements InboundUseCase {

    private final InboundRepository inboundRepository;
    private final WarehouseRepository warehouseRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryCacheEvictor cacheEvictor;

    @Override
    @Transactional
    public InboundResult createInbound(InboundCommand.CreateInbound command) {
        Warehouse warehouse = findWarehouseById(command.warehouseId());
        warehouse.validateAvailableForTransaction();

        Member handler = findMemberById(command.handlerId());
        handler.validateCanHandleInbound();

        Inbound inbound = Inbound.create(command.warehouseId(), command.handlerId(), command.supplierName());
        return InboundResult.from(inboundRepository.save(inbound));
    }

    @Override
    @Transactional
    public InboundResult addLine(Long inboundId, InboundCommand.AddLine command) {
        Inbound inbound = findInboundById(inboundId);
        productRepository.findById(command.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product", command.productId()));

        InboundLine line = InboundLine.create(command.productId(), command.orderedQuantity());
        inbound.addLine(line);
        return InboundResult.from(inboundRepository.save(inbound));
    }

    @Override
    @Transactional
    public InboundResult startInspection(Long inboundId) {
        Inbound inbound = findInboundById(inboundId);
        inbound.startInspection();
        return InboundResult.from(inboundRepository.save(inbound));
    }

    @Override
    @Transactional
    public InboundResult recordLineInspection(Long inboundId, InboundCommand.RecordInspection command) {
        Inbound inbound = findInboundById(inboundId);
        InboundLine line = inbound.getLines().stream()
                .filter(l -> l.getId().equals(command.lineId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("InboundLine", command.lineId()));
        line.recordInspectionResult(command.normalQuantity(), command.damagedQuantity(), command.pendingInspectionQuantity());
        return InboundResult.from(inboundRepository.save(inbound));
    }

    @Override
    @Transactional
    public InboundResult completeInbound(Long inboundId) {
        Inbound inbound = findInboundById(inboundId);
        inbound.complete();
        Inbound saved = inboundRepository.save(inbound);

        Warehouse warehouse = findWarehouseById(inbound.getWarehouseId());
        Long normalZoneId = warehouse.getZones().stream()
                .filter(z -> z.getZoneType() == ZoneType.NORMAL)
                .findFirst()
                .map(WarehouseZone::getId)
                .orElseThrow(() -> new IllegalStateException("NORMAL 구역이 없습니다. warehouseId=" + inbound.getWarehouseId()));
        Optional<Long> damagedZoneId = warehouse.getZones().stream()
                .filter(z -> z.getZoneType() == ZoneType.DAMAGED)
                .findFirst()
                .map(WarehouseZone::getId);

        for (InboundLine line : saved.getLines()) {
            applyInventory(inbound.getWarehouseId(), normalZoneId, damagedZoneId.orElse(normalZoneId), line);
            cacheEvictor.evictInventoryEntry(inbound.getWarehouseId(), normalZoneId, line.getProductId());
            damagedZoneId.ifPresent(dz -> cacheEvictor.evictInventoryEntry(inbound.getWarehouseId(), dz, line.getProductId()));
        }
        cacheEvictor.evictInventoryByWarehouse(inbound.getWarehouseId());

        return InboundResult.from(saved);
    }

    @Override
    @Transactional
    public InboundResult cancelInbound(Long inboundId) {
        Inbound inbound = findInboundById(inboundId);
        inbound.cancel();
        return InboundResult.from(inboundRepository.save(inbound));
    }

    @Override
    public InboundResult getInbound(Long id) {
        return InboundResult.from(findInboundById(id));
    }

    private void applyInventory(Long warehouseId, Long normalZoneId, Long damagedZoneId, InboundLine line) {
        if (normalZoneId.equals(damagedZoneId)) {
            Inventory inv = findOrCreateInventory(warehouseId, normalZoneId, line.getProductId());
            if (line.getNormalQuantity() > 0) inv.increaseNormal(line.getNormalQuantity());
            if (line.getDamagedQuantity() > 0) inv.increaseDamaged(line.getDamagedQuantity());
            if (line.getPendingInspectionQuantity() > 0) inv.increasePendingInspection(line.getPendingInspectionQuantity());
            inventoryRepository.save(inv);
        } else {
            if (line.getNormalQuantity() > 0 || line.getPendingInspectionQuantity() > 0) {
                Inventory normalInv = findOrCreateInventory(warehouseId, normalZoneId, line.getProductId());
                if (line.getNormalQuantity() > 0) normalInv.increaseNormal(line.getNormalQuantity());
                if (line.getPendingInspectionQuantity() > 0) normalInv.increasePendingInspection(line.getPendingInspectionQuantity());
                inventoryRepository.save(normalInv);
            }
            if (line.getDamagedQuantity() > 0) {
                Inventory damagedInv = findOrCreateInventory(warehouseId, damagedZoneId, line.getProductId());
                damagedInv.increaseDamaged(line.getDamagedQuantity());
                inventoryRepository.save(damagedInv);
            }
        }
    }

    private Inventory findOrCreateInventory(Long warehouseId, Long zoneId, Long productId) {
        return inventoryRepository.findByWarehouseIdAndZoneIdAndProductIdForUpdate(warehouseId, zoneId, productId)
                .orElseGet(() -> Inventory.create(warehouseId, zoneId, productId));
    }

    private Inbound findInboundById(Long id) {
        return inboundRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inbound", id));
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
