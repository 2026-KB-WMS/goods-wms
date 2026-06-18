package com.kb.wmslab.goods_wms.business.application.warehouse;

import com.kb.wmslab.goods_wms.business.domain.common.exception.EntityNotFoundException;
import com.kb.wmslab.goods_wms.business.domain.warehouse.Warehouse;
import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseRepository;
import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseZone;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
    @Caching(evict = {
            @CacheEvict(value = "warehouse", key = "#id"),
            @CacheEvict(value = "warehouseAll", allEntries = true)
    })
    public WarehouseResult activateWarehouse(Long id) {
        Warehouse warehouse = findById(id);
        warehouse.activate();
        return WarehouseResult.from(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "warehouse", key = "#id"),
            @CacheEvict(value = "warehouseAll", allEntries = true)
    })
    public WarehouseResult deactivateWarehouse(Long id) {
        Warehouse warehouse = findById(id);
        warehouse.deactivate();
        return WarehouseResult.from(warehouseRepository.save(warehouse));
    }

    @Override
    @Cacheable(value = "warehouse", key = "#id")
    public WarehouseResult getWarehouse(Long id) {
        return WarehouseResult.from(findById(id));
    }

    @Override
    @Cacheable(value = "warehouseAll", key = "'all'")
    public List<WarehouseResult> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(WarehouseResult::from)
                .collect(Collectors.toList());
    }

    private Warehouse findById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse", id));
    }
}
