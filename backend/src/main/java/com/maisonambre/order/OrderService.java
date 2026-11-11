package com.maisonambre.order;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maisonambre.boutique.Boutique;
import com.maisonambre.boutique.BoutiqueRepository;
import com.maisonambre.catalog.Inventory;
import com.maisonambre.catalog.InventoryRepository;
import com.maisonambre.common.ApiException;
import com.maisonambre.customer.ClientRepository;
import com.maisonambre.customer.Prospect;
import com.maisonambre.customer.ProspectRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final BoutiqueRepository boutiqueRepository;
    private final ProspectRepository prospectRepository;
    private final ClientRepository clientRepository;

    public OrderService(OrderRepository orderRepository,
                        InventoryRepository inventoryRepository,
                        BoutiqueRepository boutiqueRepository,
                        ProspectRepository prospectRepository,
                        ClientRepository clientRepository) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.boutiqueRepository = boutiqueRepository;
        this.prospectRepository = prospectRepository;
        this.clientRepository = clientRepository;
    }

    /**
     * Checkout: creates the order, takes the stock and completes the order straight away (there is no payment step).
     * Everything happens in one transaction: if one line fails, no stock is taken for any line.
     */
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        Long boutiqueId = request.boutiqueId();

        // 1. One order = one boutique (one stock, one currency).
        boolean mixedBoutiques = request.lines().stream()
                .anyMatch(line -> !boutiqueId.equals(line.boutiqueId()));
        if (mixedBoutiques) {
            throw ApiException.unprocessable("MIXED_BOUTIQUE_CART",
                    "All items in an order must come from boutique " + boutiqueId);
        }

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> ApiException.unprocessable("UNKNOWN_BOUTIQUE",
                        "Boutique " + boutiqueId + " does not exist"));
        Prospect prospect = prospectRepository.findById(request.customerId())
                .orElseThrow(() -> ApiException.unprocessable("UNKNOWN_CUSTOMER",
                        "Customer " + request.customerId() + " does not exist. Identify the visitor by email first"));

        // 2. Merge duplicate products and sort them by id. Two orders then always lock
        //    inventory rows in the same order, which avoids deadlocks between them.
        Map<Long, Integer> quantities = new TreeMap<>();
        for (PlaceOrderRequest.Line line : request.lines()) {
            quantities.merge(line.productId(), line.quantity(), Integer::sum);
        }

        // 3. Take the stock line by line. If a line fails, the exception rolls back the whole transaction.
        Instant now = Instant.now();
        CustomerOrder order = new CustomerOrder(boutique, prospect, now);
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();

            Inventory inventory = inventoryRepository.findInBoutique(boutiqueId, productId)
                    .orElseThrow(() -> ApiException.unprocessable("PRODUCT_NOT_SOLD_HERE",
                            "Product " + productId + " is not sold in " + boutique.getName()));

            if (inventoryRepository.decrementStock(inventory.getId(), quantity) == 0) {
                throw ApiException.conflict("INSUFFICIENT_STOCK",
                        "Not enough stock for " + inventory.getProduct().getName() + " in " + boutique.getName());
            }
            order.addLine(inventory.getProduct(), quantity, inventory.getPrice());
        }

        orderRepository.save(order);
        complete(order, now);
        return OrderResponse.from(order);
    }

    /** Completes a pending order (for example one created before checkout existed, see the seed data). */
    @Transactional
    public OrderResponse completeOrder(Long orderId) {
        CustomerOrder order = findForUpdate(orderId);
        complete(order, Instant.now());
        return OrderResponse.from(order);
    }

    /** Cancels a pending order and puts its stock back. */
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        CustomerOrder order = findForUpdate(orderId);
        order.markCancelled(Instant.now());
        for (OrderLine line : order.getLines()) {
            inventoryRepository.incrementStock(order.getBoutique().getId(), line.getProduct().getId(), line.getQuantity());
        }
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> orderNotFound(orderId));
    }

    /**
     * The only place where an order becomes a sale, and therefore the only place
     * where a prospect becomes a client. Both checkout and "complete a pending order" go through here.
     */
    private void complete(CustomerOrder order, Instant now) {
        order.markCompleted(now);
        clientRepository.convertToClient(order.getProspect().getId(), now);
    }

    private CustomerOrder findForUpdate(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId).orElseThrow(() -> orderNotFound(orderId));
    }

    private static ApiException orderNotFound(Long orderId) {
        return ApiException.notFound("ORDER_NOT_FOUND", "Order " + orderId + " not found");
    }
}
