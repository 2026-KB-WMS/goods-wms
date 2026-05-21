package com.kb.wmslab.goods_wms.business.application.product;

import com.kb.wmslab.goods_wms.business.domain.common.exception.EntityNotFoundException;
import com.kb.wmslab.goods_wms.business.domain.product.Product;
import com.kb.wmslab.goods_wms.business.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService implements ProductUseCase {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResult registerProduct(ProductCommand.Register command) {
        if (productRepository.existsBySku(command.sku())) {
            throw new IllegalArgumentException("이미 등록된 SKU입니다: " + command.sku());
        }
        Product product = Product.create(
                command.sku(), command.name(), command.characterName(),
                command.seriesName(), command.edition(), command.limitedEdition(), command.productType()
        );
        return ProductResult.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResult activateProduct(Long id) {
        Product product = findById(id);
        product.activate();
        return ProductResult.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResult deactivateProduct(Long id) {
        Product product = findById(id);
        product.deactivate();
        return ProductResult.from(productRepository.save(product));
    }

    @Override
    public ProductResult getProduct(Long id) {
        return ProductResult.from(findById(id));
    }

    private Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));
    }
}
