package com.kb.wmslab.goods_wms.business.application.warehouse;

import com.kb.wmslab.goods_wms.business.domain.common.exception.EntityNotFoundException;
import com.kb.wmslab.goods_wms.business.domain.warehouse.Warehouse;
import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseRepository;
import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseZone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService implements WarehouseUseCase {

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public WarehouseResult createWarehouse(WarehouseCommand.CreateWarehouse command) {
        Warehouse warehouse = Warehouse.create(command.name(), command.address());
        return WarehouseResult.from(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional
    public WarehouseResult addZone(Long warehouseId, WarehouseCommand.AddZone command) {
        Warehouse warehouse = findById(warehouseId);
        WarehouseZone zone = WarehouseZone.create(command.zoneCode(), command.zoneName(), command.zoneType());
        warehouse.addZone(zone);
        return WarehouseResult.from(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional
    public WarehouseResult activateWarehouse(Long id) {
        Warehouse warehouse = findById(id);
        warehouse.activate();
        return WarehouseResult.from(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional
    public WarehouseResult deactivateWarehouse(Long id) {
        Warehouse warehouse = findById(id);
        warehouse.deactivate();
        return WarehouseResult.from(warehouseRepository.save(warehouse));
    }

    @Override
    public WarehouseResult getWarehouse(Long id) {
        return WarehouseResult.from(findById(id));
    }

    private Warehouse findById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse", id));
    }
}
