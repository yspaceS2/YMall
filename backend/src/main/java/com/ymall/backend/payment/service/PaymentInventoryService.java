package com.ymall.backend.payment.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class PaymentInventoryService {

    private final ProductRepository productRepository;

    public void reserveIfNeeded(Order order) {
        if (order.isInventoryReserved()) {
            return;
        }

        Map<Long, Product> products = loadProductsForUpdate(order);
        for (OrderItem item : order.getItems()) {
            Product product = requireProduct(products, item);
            if (product.getStatus() != ProductStatus.APPROVED) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_ORDERABLE);
            }
            if (product.getStock() < item.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
            product.decreaseStock(item.getQuantity());
        }
        order.reserveInventory();
    }

    public void releaseIfReserved(Order order) {
        if (!order.isInventoryReserved()) {
            return;
        }

        Map<Long, Product> products = loadProductsForUpdate(order);
        for (OrderItem item : order.getItems()) {
            requireProduct(products, item).increaseStock(item.getQuantity());
        }
        order.releaseInventory();
    }

    public void releaseRefundableIfReserved(Order order) {
        if (!order.isInventoryReserved()) {
            return;
        }

        Map<Long, Product> products = loadProductsForUpdate(order);
        for (OrderItem item : order.getItems()) {
            int refundableQuantity = item.getRefundableQuantity();
            if (refundableQuantity > 0) {
                requireProduct(products, item).increaseStock(refundableQuantity);
            }
        }
        order.releaseInventory();
    }

    private Map<Long, Product> loadProductsForUpdate(Order order) {
        List<Long> productIds = order.getItems().stream()
            .map(item -> item.getProduct().getId())
            .sorted()
            .toList();
        return productRepository.findAllByIdForUpdate(productIds)
            .stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Product requireProduct(Map<Long, Product> products, OrderItem item) {
        Product product = products.get(item.getProduct().getId());
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }
}
