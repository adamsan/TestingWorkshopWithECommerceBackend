package com.example.ecommerce.backend.service;

import com.example.ecommerce.backend.dao.OrderDao;
import com.example.ecommerce.backend.dao.ProductDao;
import com.example.ecommerce.backend.dao.UserDao;
import com.example.ecommerce.backend.model.Order;
import com.example.ecommerce.backend.model.Product;
import com.example.ecommerce.backend.model.User;
import com.example.ecommerce.backend.requests.OrderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderDao orderDao;
    @Mock
    private UserDao userDao;
    @Mock
    private ProductDao productDao;

    @Captor
    ArgumentCaptor<Order> orderArgumentCaptor;


    @InjectMocks
    private OrderService service;

    @Test
    void findAllShouldReturnWithOrderDaoResponse() {
        List<Order> orders = List.of(new Order(
                1L,
                new Product(),
                4,
                LocalDate.now().plus(5, DAYS),
                BigDecimal.valueOf(333),
                new User())
        );
        when(orderDao.findAll()).thenReturn(orders);

        List<Order> actualOrders = service.findAll();
        assertEquals(orders, actualOrders);

        verify(orderDao, times(1)).findAll();
        verifyNoInteractions(productDao);
        verifyNoInteractions(userDao);
    }

    @Test
    void saveNewOrderShouldDecreaseProductInStock() {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setId(null);
        orderRequest.setOrderDate(LocalDate.now().plus(1, DAYS));
        orderRequest.setTotal(BigDecimal.valueOf(12_200L));
        final int orderQuantity = 3;
        orderRequest.setQuantity(orderQuantity);
        final long userId = 1L;
        orderRequest.setUserId(userId);
        final long productId = 2L;
        orderRequest.setProductId(productId);

        User user = User.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .email("jd@yahoo.com")
                .password("1234")
                .build();

        final int productInStock = 5;
        Product product = Product.builder()
                .id(productId)
                .description("...")
                .name("Dalek Plunger")
                .inStock(productInStock)
                .price(BigDecimal.valueOf(10L))
                .build();

        when(userDao.findById(userId)).thenReturn(Optional.of(user));
        when(productDao.findById(productId)).thenReturn(Optional.of(product));
        when(orderDao.save(any())).thenAnswer(a -> a.getArgument(0));

        Order order = service.save(orderRequest);

        assertEquals(productInStock - orderQuantity, product.getInStock());
        verify(productDao, times(1)).save(product);
        verify(orderDao, times(1)).save(order);
    }


    @Test
    void saveExistingOrderShouldDecreaseProductInStockOnlyWithTheDifference() {
        final long orderId = 1L;
        final long userId = 1L;
        final long productId = 2L;
        final int previousOrderQuantity = 3;
        final int productInStock = 5;
        final int orderQuantity = 3;


        User user = User.builder()
                .id(userId).firstName("John").lastName("Doe").password("123")
                .build();
        Product product = Product.builder()
                .id(productId).name("Plunger").inStock(productInStock).price(BigDecimal.valueOf(10L))
                .build();

        Order previousOrder = Order.builder()
                .id(orderId).orderDate(LocalDate.now().plus(1, DAYS))
                .total(BigDecimal.valueOf(12_200L)).quantity(previousOrderQuantity).user(user).product(product)
                .build();

        when(orderDao.findById(orderId)).thenReturn(Optional.of(previousOrder));

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setId(1L);
        orderRequest.setOrderDate(LocalDate.now().plus(1, DAYS));
        orderRequest.setTotal(BigDecimal.valueOf(12_200L));
        orderRequest.setQuantity(orderQuantity);
        orderRequest.setUserId(userId);
        orderRequest.setProductId(productId);

        when(userDao.findById(userId)).thenReturn(Optional.of(user));
        when(productDao.findById(productId)).thenReturn(Optional.of(product));

        when(orderDao.save(orderArgumentCaptor.capture())).thenAnswer(a -> a.getArgument(0));
        Order order = service.save(orderRequest);

        assertEquals(productInStock - orderQuantity + previousOrderQuantity, product.getInStock());
        verify(productDao, times(1)).save(product);
        verify(orderDao, times(1)).save(order);
    }


    @Test
    void saveNewOrderShouldFailWhenLessProductsAreInStockThenOrdered() {
        final long orderId = 1L;
        final long userId = 1L;
        final long productId = 2L;
        final int previousOrderQuantity = 3;
        final int productInStock = 5;
        final int orderQuantity = 10;


        User user = User.builder()
                .id(userId).firstName("John").lastName("Doe").password("123")
                .build();
        Product product = Product.builder()
                .id(productId).name("Plunger").inStock(productInStock).price(BigDecimal.valueOf(10L))
                .build();

        Order previousOrder = Order.builder()
                .id(orderId).orderDate(LocalDate.now().plus(1, DAYS))
                .total(BigDecimal.valueOf(12_200L)).quantity(previousOrderQuantity).user(user).product(product)
                .build();

        when(orderDao.findById(orderId)).thenReturn(Optional.of(previousOrder));

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setId(1L);
        orderRequest.setOrderDate(LocalDate.now().plus(1, DAYS));
        orderRequest.setTotal(BigDecimal.valueOf(12_200L));
        orderRequest.setQuantity(orderQuantity);
        orderRequest.setUserId(userId);
        orderRequest.setProductId(productId);

        when(userDao.findById(userId)).thenReturn(Optional.of(user));
        when(productDao.findById(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () -> service.save(orderRequest));
    }
}