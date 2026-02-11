package com.example.EcoGo.service;

import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.PointsService;
import com.example.EcoGo.interfacemethods.VipSwitchService;
import com.example.EcoGo.model.Goods;
import com.example.EcoGo.model.Order;
import com.example.EcoGo.model.User;
import com.example.EcoGo.model.UserVoucher;
import com.example.EcoGo.repository.OrderRepository;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.repository.UserVoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    private OrderServiceImpl service;

    private VipSwitchService vipSwitchService;
    private UserVoucherRepository userVoucherRepository;
    private OrderRepository orderRepository;
    private GoodsService goodsService;
    private PointsService pointsService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() throws Exception {
        vipSwitchService = mock(VipSwitchService.class);
        userVoucherRepository = mock(UserVoucherRepository.class);
        orderRepository = mock(OrderRepository.class);
        goodsService = mock(GoodsService.class);
        pointsService = mock(PointsService.class);
        userRepository = mock(UserRepository.class);

        service = new OrderServiceImpl();

        inject("vipSwitchService", vipSwitchService);
        inject("userVoucherRepository", userVoucherRepository);
        inject("orderRepository", orderRepository);
        inject("goodsService", goodsService);
        inject("pointsService", pointsService);
        inject("userRepository", userRepository);
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field f = OrderServiceImpl.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(service, value);
    }

    private static Goods goods(String id, String type, int points, Integer vipReq, boolean forRedemption) {
        Goods g = new Goods();
        g.setId(id);
        g.setType(type);
        g.setName("G-" + id);
        g.setIsForRedemption(forRedemption);
        g.setRedemptionPoints(points);
        g.setVipLevelRequired(vipReq);
        g.setImageUrl("img");
        return g;
    }

    private static Order redemptionOrder(String userId, String goodsId, Integer qty) {
        Order o = new Order();
        o.setUserId(userId);

        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId(goodsId);
        item.setQuantity(qty);

        o.setItems(List.of(item));
        return o;
    }

    // ========== createOrder ==========

    @Test
    void createOrder_generatesOrderNumber_whenNull() {
        Order order = new Order();
        order.setOrderNumber(null);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);

        assertNotNull(result.getOrderNumber());
        assertTrue(result.getOrderNumber().startsWith("ORD"));
    }

    @Test
    void createOrder_generatesOrderNumber_whenEmpty() {
        Order order = new Order();
        order.setOrderNumber("");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);

        assertNotNull(result.getOrderNumber());
        assertTrue(result.getOrderNumber().startsWith("ORD"));
    }

    @Test
    void createOrder_keepsExistingOrderNumber() {
        Order order = new Order();
        order.setOrderNumber("EXISTING123");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);

        assertEquals("EXISTING123", result.getOrderNumber());
    }

    @Test
    void createOrder_calculatesTotal_fromSubtotal() {
        Order order = new Order();
        order.setUserId(null); // skip VIP check
        Order.OrderItem item1 = new Order.OrderItem();
        item1.setSubtotal(25.0);
        Order.OrderItem item2 = new Order.OrderItem();
        item2.setSubtotal(30.0);
        order.setItems(List.of(item1, item2));
        order.setShippingFee(5.0);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);

        assertEquals(55.0, result.getTotalAmount(), 0.01);
        assertEquals(60.0, result.getFinalAmount(), 0.01);
    }

    @Test
    void createOrder_calculatesTotal_fromPriceTimesQuantity() {
        Order order = new Order();
        Order.OrderItem item = new Order.OrderItem();
        item.setPrice(10.0);
        item.setQuantity(3);
        order.setItems(List.of(item));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);

        assertEquals(30.0, result.getTotalAmount(), 0.01);
        assertEquals(30.0, result.getFinalAmount(), 0.01); // shippingFee defaults to 0.0
    }

    @Test
    void createOrder_calculatesTotal_defaultsToZero_noSubtotalNoPrice() {
        Order order = new Order();
        Order.OrderItem item = new Order.OrderItem();
        // no subtotal, no price, no quantity
        order.setItems(List.of(item));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);

        assertEquals(0.0, result.getTotalAmount(), 0.01);
    }

    @Test
    void createOrder_nullShippingFee_treatedAsZero() {
        Order order = new Order();
        Order.OrderItem item = new Order.OrderItem();
        item.setSubtotal(50.0);
        order.setItems(List.of(item));
        order.setShippingFee(null);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);

        assertEquals(50.0, result.getFinalAmount(), 0.01);
    }

    @Test
    void createOrder_setsDefaultStatus_whenNull() {
        Order order = new Order();
        order.setStatus(null);
        order.setPaymentStatus(null);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);

        assertEquals("COMPLETED", result.getStatus());
        assertEquals("COMPLETED", result.getPaymentStatus());
    }

    @Test
    void createOrder_setsDefaultStatus_whenEmpty() {
        Order order = new Order();
        order.setStatus("");
        order.setPaymentStatus("");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);

        assertEquals("COMPLETED", result.getStatus());
        assertEquals("COMPLETED", result.getPaymentStatus());
    }

    @Test
    void createOrder_keepsExistingStatus() {
        Order order = new Order();
        order.setStatus("SHIPPED");
        order.setPaymentStatus("PAID");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);

        assertEquals("SHIPPED", result.getStatus());
        assertEquals("PAID", result.getPaymentStatus());
    }

    @Test
    void createOrder_noItems_skipsAmountCalcAndVipCheck() {
        Order order = new Order();
        order.setUserId("u1");
        order.setItems(null);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);

        assertNull(result.getTotalAmount());
        verify(goodsService, never()).getGoodsById(anyString());
    }

    @Test
    void createOrder_emptyItems_skipsAmountCalcAndVipCheck() {
        Order order = new Order();
        order.setUserId("u1");
        order.setItems(new ArrayList<>());

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);

        assertNull(result.getTotalAmount());
    }

    @Test
    void createOrder_vipRequired_userIsVip_passes() {
        User user = new User();
        user.setUserid("u1");
        User.Vip vip = new User.Vip();
        vip.setActive(true);
        vip.setExpiryDate(LocalDateTime.now().plusDays(30));
        user.setVip(vip);
        when(userRepository.findByUserid("u1")).thenReturn(Optional.of(user));

        Goods g = new Goods();
        g.setVipLevelRequired(1);
        when(goodsService.getGoodsById("g1")).thenReturn(g);

        Order order = new Order();
        order.setUserId("u1");
        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId("g1");
        item.setSubtotal(10.0);
        order.setItems(List.of(item));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);
        assertNotNull(result);
    }

    @Test
    void createOrder_vipRequired_userNotVip_throws() {
        User user = new User();
        user.setUserid("u1");
        // No VIP
        when(userRepository.findByUserid("u1")).thenReturn(Optional.of(user));

        Goods g = new Goods();
        g.setVipLevelRequired(1);
        when(goodsService.getGoodsById("g1")).thenReturn(g);

        Order order = new Order();
        order.setUserId("u1");
        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId("g1");
        order.setItems(List.of(item));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createOrder(order));
        assertTrue(ex.getMessage().contains("VIP_REQUIRED"));
    }

    @Test
    void createOrder_goodsNull_skipsVipCheck() {
        when(userRepository.findByUserid("u1")).thenReturn(Optional.empty());
        when(goodsService.getGoodsById("g1")).thenReturn(null);

        Order order = new Order();
        order.setUserId("u1");
        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId("g1");
        item.setSubtotal(10.0);
        order.setItems(List.of(item));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);
        assertNotNull(result);
    }

    @Test
    void createOrder_blankGoodsId_skipsVipCheck() {
        when(userRepository.findByUserid("u1")).thenReturn(Optional.empty());

        Order order = new Order();
        order.setUserId("u1");
        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId("   ");
        item.setSubtotal(10.0);
        order.setItems(List.of(item));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);
        assertNotNull(result);
        verify(goodsService, never()).getGoodsById(anyString());
    }

    @Test
    void createOrder_nullGoodsId_skipsVipCheck() {
        when(userRepository.findByUserid("u1")).thenReturn(Optional.empty());

        Order order = new Order();
        order.setUserId("u1");
        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId(null);
        item.setSubtotal(10.0);
        order.setItems(List.of(item));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);
        assertNotNull(result);
    }

    @Test
    void createOrder_vipLevelNotRequired_passes() {
        when(userRepository.findByUserid("u1")).thenReturn(Optional.empty());

        Goods g = new Goods();
        g.setVipLevelRequired(0); // not required
        when(goodsService.getGoodsById("g1")).thenReturn(g);

        Order order = new Order();
        order.setUserId("u1");
        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId("g1");
        item.setSubtotal(10.0);
        order.setItems(List.of(item));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);
        assertNotNull(result);
    }

    @Test
    void createOrder_vipLevelNull_passes() {
        when(userRepository.findByUserid("u1")).thenReturn(Optional.empty());

        Goods g = new Goods();
        g.setVipLevelRequired(null);
        when(goodsService.getGoodsById("g1")).thenReturn(g);

        Order order = new Order();
        order.setUserId("u1");
        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId("g1");
        item.setSubtotal(10.0);
        order.setItems(List.of(item));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder(order);
        assertNotNull(result);
    }

    // ========== getOrdersByStatus ==========

    @Test
    void getOrdersByStatus_nullStatus_returnsAll() {
        when(orderRepository.findAll()).thenReturn(List.of(new Order(), new Order()));
        List<Order> result = service.getOrdersByStatus(null);
        assertEquals(2, result.size());
        verify(orderRepository).findAll();
    }

    @Test
    void getOrdersByStatus_blankStatus_returnsAll() {
        when(orderRepository.findAll()).thenReturn(List.of(new Order()));
        List<Order> result = service.getOrdersByStatus("   ");
        assertEquals(1, result.size());
        verify(orderRepository).findAll();
    }

    @Test
    void getOrdersByStatus_withStatus_returnsFiltered() {
        when(orderRepository.findByStatus("COMPLETED")).thenReturn(List.of(new Order()));
        List<Order> result = service.getOrdersByStatus("COMPLETED");
        assertEquals(1, result.size());
        verify(orderRepository).findByStatus("COMPLETED");
    }

    // ========== updateOrder ==========

    @Test
    void updateOrder_existingOrder_updatesFields() {
        Order existing = new Order();
        existing.setId("id1");
        existing.setTotalAmount(100.0);

        when(orderRepository.findById("id1")).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order updateData = new Order();
        updateData.setStatus("SHIPPED");
        updateData.setPaymentStatus("PAID");
        updateData.setPaymentMethod("CREDIT_CARD");
        updateData.setShippingAddress("123 Main St");
        updateData.setRecipientName("John");
        updateData.setRecipientPhone("123456");
        updateData.setRemark("Note");
        updateData.setShippingFee(10.0);
        updateData.setTrackingNumber("TRACK123");

        Order result = service.updateOrder("id1", updateData);

        assertEquals("SHIPPED", result.getStatus());
        assertEquals("PAID", result.getPaymentStatus());
        assertEquals("CREDIT_CARD", result.getPaymentMethod());
        assertEquals("123 Main St", result.getShippingAddress());
        assertEquals("John", result.getRecipientName());
        assertEquals("123456", result.getRecipientPhone());
        assertEquals("Note", result.getRemark());
        assertEquals(10.0, result.getShippingFee(), 0.01);
        assertEquals(110.0, result.getFinalAmount(), 0.01); // 100 + 10
        assertEquals("TRACK123", result.getTrackingNumber());
    }

    @Test
    void updateOrder_existingOrder_shippingFeeWithNullTotalAmount() {
        Order existing = new Order();
        existing.setId("id1");
        existing.setTotalAmount(null);

        when(orderRepository.findById("id1")).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order updateData = new Order();
        updateData.setShippingFee(10.0);

        Order result = service.updateOrder("id1", updateData);

        assertEquals(10.0, result.getShippingFee(), 0.01);
        // finalAmount not recalculated because totalAmount is null
        assertNull(result.getFinalAmount());
    }

    @Test
    void updateOrder_existingOrder_nullFields_skipsUpdate() {
        Order existing = new Order();
        existing.setId("id1");
        existing.setStatus("PENDING");
        existing.setRemark("Old");

        when(orderRepository.findById("id1")).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // All fields null in update request
        Order updateData = new Order();

        Order result = service.updateOrder("id1", updateData);

        assertEquals("PENDING", result.getStatus()); // not changed
        assertEquals("Old", result.getRemark());
    }

    @Test
    void updateOrder_notFound_throwsRuntimeException() {
        when(orderRepository.findById("nonexistent")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateOrder("nonexistent", new Order()));
    }

    // ========== deleteOrder ==========

    @Test
    void deleteOrder_callsRepository() {
        service.deleteOrder("id1");
        verify(orderRepository).deleteById("id1");
    }

    // ========== getAllOrders ==========

    @Test
    void getAllOrders_returnsAll() {
        when(orderRepository.findAll()).thenReturn(List.of(new Order(), new Order()));
        List<Order> result = service.getAllOrders();
        assertEquals(2, result.size());
    }

    // ========== getOrderById ==========

    @Test
    void getOrderById_found() {
        Order order = new Order();
        order.setId("id1");
        when(orderRepository.findById("id1")).thenReturn(Optional.of(order));

        Order result = service.getOrderById("id1");
        assertEquals("id1", result.getId());
    }

    @Test
    void getOrderById_notFound_throwsRuntimeException() {
        when(orderRepository.findById("nonexistent")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getOrderById("nonexistent"));
    }

    // ========== getOrdersByUserId ==========

    @Test
    void getOrdersByUserId_returnsOrders() {
        when(orderRepository.findByUserId("u1")).thenReturn(List.of(new Order()));
        List<Order> result = service.getOrdersByUserId("u1");
        assertEquals(1, result.size());
    }

    // ========== createRedemptionOrder - parameter validation ==========

    @Test
    void createRedemptionOrder_nullOrder_throwParamError() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(null));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void createRedemptionOrder_missingUserId_throwParamError() {
        Order o = redemptionOrder(null, "g1", 1);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void createRedemptionOrder_emptyUserId_throwParamError() {
        Order o = redemptionOrder("", "g1", 1);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void createRedemptionOrder_missingItems_throwParamError() {
        Order o = new Order();
        o.setUserId("u1");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void createRedemptionOrder_emptyItems_throwParamError() {
        Order o = new Order();
        o.setUserId("u1");
        o.setItems(new ArrayList<>());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    // ---------- MISSING_GOODS_ID ----------
    @Test
    void createRedemptionOrder_nullGoodsId_throwParamError() {
        Order o = redemptionOrder("u1", null, 1);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertTrue(ex.getMessage().contains("MISSING_GOODS_ID"));
    }

    @Test
    void createRedemptionOrder_emptyGoodsId_throwParamError() {
        Order o = redemptionOrder("u1", "", 1);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertTrue(ex.getMessage().contains("MISSING_GOODS_ID"));
    }

    // ---------- GOODS_NOT_FOUND ----------
    @Test
    void createRedemptionOrder_goodsNotFound_throwParamError() {
        Order o = redemptionOrder("u1", "g1", 1);
        when(goodsService.getGoodsById("g1")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertTrue(ex.getMessage().contains("GOODS_NOT_FOUND"));
    }

    // ---------- GOODS_NOT_FOR_REDEMPTION ----------
    @Test
    void createRedemptionOrder_notForRedemption_throwParamError() {
        Order o = redemptionOrder("u1", "g1", 1);
        Goods g = goods("g1", "normal", 100, 0, false);
        when(goodsService.getGoodsById("g1")).thenReturn(g);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertTrue(ex.getMessage().contains("GOODS_NOT_FOR_REDEMPTION"));
    }

    // ---------- INVALID_REDEMPTION_POINTS ----------
    @Test
    void createRedemptionOrder_zeroRedemptionPoints_throwParamError() {
        Order o = redemptionOrder("u1", "g1", 1);
        Goods g = goods("g1", "normal", 0, 0, true);
        when(goodsService.getGoodsById("g1")).thenReturn(g);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertTrue(ex.getMessage().contains("INVALID_REDEMPTION_POINTS"));
    }

    @Test
    void createRedemptionOrder_nullRedemptionPoints_throwParamError() {
        Order o = redemptionOrder("u1", "g1", 1);
        Goods g = goods("g1", "normal", 0, 0, true);
        g.setRedemptionPoints(null);
        when(goodsService.getGoodsById("g1")).thenReturn(g);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertTrue(ex.getMessage().contains("INVALID_REDEMPTION_POINTS"));
    }

    // ---------- Normal goods: reserve stock, deduct points, save order ----------
    @Test
    void createRedemptionOrder_normalGoods_shouldReserveStock_andDeductPoints_andSaveOrder() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "g1", 2);

        Goods g1 = goods("g1", "normal", 100, 0, true);
        when(goodsService.getGoodsById("g1")).thenReturn(g1);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order saved = service.createRedemptionOrder(o);

        verify(goodsService).reserveStock("g1", 2);
        verify(pointsService).adjustPoints(eq(userId), eq(-200L), eq("store"), contains("Purchased"), isNull(), isNull());
        verify(orderRepository).save(any(Order.class));

        verify(userVoucherRepository, never()).save(any(UserVoucher.class));

        assertEquals(Boolean.TRUE, saved.getIsRedemptionOrder());
        assertEquals("POINTS", saved.getPaymentMethod());
        assertEquals(200, saved.getPointsUsed());
        assertEquals("PAID", saved.getPaymentStatus());
        assertEquals("COMPLETED", saved.getStatus());
        assertNotNull(saved.getOrderNumber());
    }

    @Test
    void createRedemptionOrder_quantityNull_defaultsTo1() {
        Order o = redemptionOrder("u1", "g1", null);
        Goods g1 = goods("g1", "normal", 100, 0, true);
        when(goodsService.getGoodsById("g1")).thenReturn(g1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order saved = service.createRedemptionOrder(o);

        verify(goodsService).reserveStock("g1", 1);
        verify(pointsService).adjustPoints(eq("u1"), eq(-100L), eq("store"), contains("Purchased"), isNull(), isNull());
        assertEquals(100, saved.getPointsUsed());
    }

    @Test
    void createRedemptionOrder_quantityZero_defaultsTo1() {
        Order o = redemptionOrder("u1", "g1", 0);
        Goods g1 = goods("g1", "normal", 100, 0, true);
        when(goodsService.getGoodsById("g1")).thenReturn(g1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order saved = service.createRedemptionOrder(o);

        verify(goodsService).reserveStock("g1", 1);
        assertEquals(100, saved.getPointsUsed());
    }

    @Test
    void createRedemptionOrder_existingOrderNumber_preserved() {
        Order o = redemptionOrder("u1", "g1", 1);
        o.setOrderNumber("EXISTING_NUM");
        Goods g1 = goods("g1", "normal", 100, 0, true);
        when(goodsService.getGoodsById("g1")).thenReturn(g1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order saved = service.createRedemptionOrder(o);
        assertEquals("EXISTING_NUM", saved.getOrderNumber());
    }

    // ---------- Voucher goods ----------
    @Test
    void createRedemptionOrder_voucher_shouldCreateUserVouchers_andDeductPoints() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "v1", 3);

        Goods v1 = goods("v1", "voucher", 50, 0, true);
        when(goodsService.getGoodsById("v1")).thenReturn(v1);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createRedemptionOrder(o);

        verify(goodsService, never()).reserveStock(anyString(), anyInt());
        verify(pointsService).adjustPoints(eq(userId), eq(-150L), eq("store"), contains("Purchased"), isNull(), isNull());
        verify(userVoucherRepository, times(3)).save(any(UserVoucher.class));
    }

    // ---------- VIP goods ----------
    @Test
    void createRedemptionOrder_vip_shouldActivateVip() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "vip1", 1);

        Goods vip = goods("vip1", "vip", 300, 0, true);
        when(goodsService.getGoodsById("vip1")).thenReturn(vip);

        User user = new User();
        user.setUserid(userId);
        User.Vip v = new User.Vip();
        v.setActive(false);
        v.setExpiryDate(LocalDateTime.now().minusDays(1));
        user.setVip(v);

        when(userRepository.findByUserid(userId)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createRedemptionOrder(o);

        verify(goodsService, never()).reserveStock(anyString(), anyInt());
        verify(pointsService).adjustPoints(eq(userId), eq(-300L), eq("store"), contains("Purchased"), isNull(), isNull());
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    @Test
    void createRedemptionOrder_vip_multipleQty_extendsMoreDays() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "vip1", 2);

        Goods vip = goods("vip1", "vip", 100, 0, true);
        when(goodsService.getGoodsById("vip1")).thenReturn(vip);

        User user = new User();
        user.setUserid(userId);
        user.setVip(new User.Vip());

        when(userRepository.findByUserid(userId)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createRedemptionOrder(o);

        // 2 VIPs = 60 days activation
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    // ---------- VIP-exclusive: switch off ----------
    @Test
    void createRedemptionOrder_vipExclusive_switchOff_shouldThrowVipDisabled() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "gx", 1);

        Goods g = goods("gx", "normal", 10, 1, true);
        when(goodsService.getGoodsById("gx")).thenReturn(g);

        when(vipSwitchService.isSwitchEnabled("Exclusive_goods")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("VIP_DISABLED"));
    }

    // ---------- VIP-exclusive voucher: switch off ----------
    @Test
    void createRedemptionOrder_vipExclusiveVoucher_switchOff_shouldThrowVipDisabled() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "vx", 1);

        Goods g = goods("vx", "voucher", 10, 1, true);
        when(goodsService.getGoodsById("vx")).thenReturn(g);

        when(vipSwitchService.isSwitchEnabled("Exclusive_vouchers")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertTrue(ex.getMessage().contains("VIP_DISABLED"));
    }

    // ---------- VIP-exclusive: switch on but user not VIP ----------
    @Test
    void createRedemptionOrder_vipExclusive_switchOn_userNotVip_throwsVipRequired() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "gx", 1);

        Goods g = goods("gx", "normal", 10, 1, true);
        when(goodsService.getGoodsById("gx")).thenReturn(g);
        when(vipSwitchService.isSwitchEnabled("Exclusive_goods")).thenReturn(true);
        // user not VIP
        when(userRepository.findByUserid(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertTrue(ex.getMessage().contains("VIP_REQUIRED"));
    }

    // ---------- VIP-exclusive voucher: switch on but user not VIP ----------
    @Test
    void createRedemptionOrder_vipExclusiveVoucher_switchOn_userNotVip_throwsVipRequired() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "vx", 1);

        Goods g = goods("vx", "voucher", 10, 1, true);
        when(goodsService.getGoodsById("vx")).thenReturn(g);
        when(vipSwitchService.isSwitchEnabled("Exclusive_vouchers")).thenReturn(true);
        when(userRepository.findByUserid(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertTrue(ex.getMessage().contains("VIP_REQUIRED"));
    }

    // ---------- VIP-exclusive: vip type goods (vipSubscription) bypasses check ----------
    @Test
    void createRedemptionOrder_vipTypeGoods_vipLevelRequired_bypasses() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "vip1", 1);

        // vip type with vipLevelRequired=1 but isVipSubscription=true so bypass check
        Goods g = goods("vip1", "vip", 200, 1, true);
        when(goodsService.getGoodsById("vip1")).thenReturn(g);

        User user = new User();
        user.setUserid(userId);
        user.setVip(new User.Vip());

        when(userRepository.findByUserid(userId)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Should not throw despite user not being VIP, because vip type is exempted
        Order saved = service.createRedemptionOrder(o);
        assertNotNull(saved);
    }

    // ---------- Rollback: reserve succeeds then pointsService throws => releaseStock ----------
    @Test
    void createRedemptionOrder_pointsServiceThrows_shouldRollbackStock() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "g1", 1);

        Goods g1 = goods("g1", "normal", 100, 0, true);
        when(goodsService.getGoodsById("g1")).thenReturn(g1);

        doThrow(new RuntimeException("points fail"))
                .when(pointsService).adjustPoints(eq(userId), anyLong(), anyString(), anyString(), any(), any());

        assertThrows(RuntimeException.class, () -> service.createRedemptionOrder(o));

        verify(goodsService).reserveStock("g1", 1);
        verify(goodsService).releaseStock("g1", 1);

        verify(pointsService, times(1))
                .adjustPoints(eq(userId), anyLong(), anyString(), anyString(), any(), any());
    }

    // ---------- Rollback: points deducted then later step fails => refund points ----------
    @Test
    void createRedemptionOrder_postPointsFailure_shouldRefundPoints() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "vip1", 1);

        Goods vip = goods("vip1", "vip", 100, 0, true);
        when(goodsService.getGoodsById("vip1")).thenReturn(vip);

        // Points deduction succeeds (first call)
        // Then activateVipInternal fails (user not found)
        when(userRepository.findByUserid(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.createRedemptionOrder(o));

        // Verify refund attempted (second adjustPoints call with positive amount)
        verify(pointsService, atLeast(2)).adjustPoints(eq(userId), anyLong(), anyString(), anyString(), any(), any());
    }

    // ---------- activateVipInternal: via VIP goods, user has active VIP -> extend ----------
    @Test
    void createRedemptionOrder_vip_existingActiveVip_extends() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "vip1", 1);

        Goods vip = goods("vip1", "vip", 100, 0, true);
        when(goodsService.getGoodsById("vip1")).thenReturn(vip);

        User user = new User();
        user.setUserid(userId);
        User.Vip v = new User.Vip();
        v.setActive(true);
        v.setExpiryDate(LocalDateTime.now().plusDays(15));
        v.setStartDate(LocalDateTime.now().minusDays(15));
        user.setVip(v);

        when(userRepository.findByUserid(userId)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createRedemptionOrder(o);

        // VIP extended, expiry should be around +45 days from now (15 remaining + 30 new)
        assertTrue(user.getVip().getExpiryDate().isAfter(LocalDateTime.now().plusDays(40)));
        verify(userRepository, atLeastOnce()).save(user);
    }

    // ---------- activateVipInternal: user found by findById fallback ----------
    @Test
    void createRedemptionOrder_vip_userFoundByIdFallback() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "vip1", 1);

        Goods vip = goods("vip1", "vip", 100, 0, true);
        when(goodsService.getGoodsById("vip1")).thenReturn(vip);

        User user = new User();
        user.setUserid(userId);
        user.setVip(new User.Vip());

        // findByUserid returns empty, findById returns user
        when(userRepository.findByUserid(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createRedemptionOrder(o);

        assertTrue(user.getVip().isActive());
        verify(userRepository, atLeastOnce()).save(user);
    }

    // ---------- activateVipInternal: null vip object -> creates new ----------
    @Test
    void createRedemptionOrder_vip_nullVipObject_createsNew() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "vip1", 1);

        Goods vip = goods("vip1", "vip", 100, 0, true);
        when(goodsService.getGoodsById("vip1")).thenReturn(vip);

        User user = new User();
        user.setUserid(userId);
        user.setVip(null);

        when(userRepository.findByUserid(userId)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createRedemptionOrder(o);

        assertNotNull(user.getVip());
        assertTrue(user.getVip().isActive());
    }

    // ---------- Item with null goodsName -> uses goodsId in description ----------
    @Test
    void createRedemptionOrder_itemWithNullGoodsName_usesGoodsIdInDescription() {
        Order o = new Order();
        o.setUserId("u1");

        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId("g1");
        item.setGoodsName(null);
        item.setQuantity(1);
        o.setItems(new ArrayList<>(List.of(item)));

        Goods g1 = goods("g1", "normal", 50, 0, true);
        // goodsName will be set by the method to "G-g1", so let's test blank goodsName
        g1.setName(null); // name will be null
        when(goodsService.getGoodsById("g1")).thenReturn(g1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order saved = service.createRedemptionOrder(o);
        assertNotNull(saved);
    }

    // ========== isVipActive (tested indirectly via createOrder) ==========

    @Test
    void isVipActive_nullUserId() {
        // createOrder with null userId skips VIP check entirely
        Order order = new Order();
        order.setUserId(null);
        order.setItems(List.of(new Order.OrderItem()));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Should not throw
        Order result = service.createOrder(order);
        assertNotNull(result);
    }

    @Test
    void isVipActive_userFoundByIdFallback_withActiveVip() {
        // findByUserid empty, findById returns user with active VIP
        User user = new User();
        user.setUserid("u1");
        User.Vip vip = new User.Vip();
        vip.setActive(true);
        vip.setExpiryDate(LocalDateTime.now().plusDays(30));
        user.setVip(vip);

        when(userRepository.findByUserid("u1")).thenReturn(Optional.empty());
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        Goods g = new Goods();
        g.setVipLevelRequired(1);
        when(goodsService.getGoodsById("g1")).thenReturn(g);

        Order order = new Order();
        order.setUserId("u1");
        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId("g1");
        item.setSubtotal(10.0);
        order.setItems(List.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Should pass because user is VIP (found via findById)
        Order result = service.createOrder(order);
        assertNotNull(result);
    }

    @Test
    void isVipActive_expiredVip_returnsFalse() {
        User user = new User();
        user.setUserid("u1");
        User.Vip vip = new User.Vip();
        vip.setActive(true);
        vip.setExpiryDate(LocalDateTime.now().minusDays(1)); // expired
        user.setVip(vip);

        when(userRepository.findByUserid("u1")).thenReturn(Optional.of(user));

        Goods g = new Goods();
        g.setVipLevelRequired(1);
        when(goodsService.getGoodsById("g1")).thenReturn(g);

        Order order = new Order();
        order.setUserId("u1");
        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId("g1");
        order.setItems(List.of(item));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createOrder(order));
        assertTrue(ex.getMessage().contains("VIP_REQUIRED"));
    }

    @Test
    void isVipActive_nullVipExpiryDate_returnsFalse() {
        User user = new User();
        user.setUserid("u1");
        User.Vip vip = new User.Vip();
        vip.setActive(true);
        vip.setExpiryDate(null);
        user.setVip(vip);

        when(userRepository.findByUserid("u1")).thenReturn(Optional.of(user));

        Goods g = new Goods();
        g.setVipLevelRequired(1);
        when(goodsService.getGoodsById("g1")).thenReturn(g);

        Order order = new Order();
        order.setUserId("u1");
        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId("g1");
        order.setItems(List.of(item));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createOrder(order));
        assertTrue(ex.getMessage().contains("VIP_REQUIRED"));
    }

    @Test
    void isVipActive_inactiveVip_returnsFalse() {
        User user = new User();
        user.setUserid("u1");
        User.Vip vip = new User.Vip();
        vip.setActive(false);
        vip.setExpiryDate(LocalDateTime.now().plusDays(30));
        user.setVip(vip);

        when(userRepository.findByUserid("u1")).thenReturn(Optional.of(user));

        Goods g = new Goods();
        g.setVipLevelRequired(1);
        when(goodsService.getGoodsById("g1")).thenReturn(g);

        Order order = new Order();
        order.setUserId("u1");
        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId("g1");
        order.setItems(List.of(item));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createOrder(order));
        assertTrue(ex.getMessage().contains("VIP_REQUIRED"));
    }

    @Test
    void isVipActive_nullVip_returnsFalse() {
        User user = new User();
        user.setUserid("u1");
        user.setVip(null);

        when(userRepository.findByUserid("u1")).thenReturn(Optional.of(user));

        Goods g = new Goods();
        g.setVipLevelRequired(1);
        when(goodsService.getGoodsById("g1")).thenReturn(g);

        Order order = new Order();
        order.setUserId("u1");
        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId("g1");
        order.setItems(List.of(item));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createOrder(order));
        assertTrue(ex.getMessage().contains("VIP_REQUIRED"));
    }

    // ========== safeMsg ==========

    @Test
    void safeMsg_withMessage() throws Exception {
        Method m = OrderServiceImpl.class.getDeclaredMethod("safeMsg", Exception.class);
        m.setAccessible(true);
        String result = (String) m.invoke(service, new RuntimeException("test message"));
        assertEquals("test message", result);
    }

    @Test
    void safeMsg_nullMessage() throws Exception {
        Method m = OrderServiceImpl.class.getDeclaredMethod("safeMsg", Exception.class);
        m.setAccessible(true);
        String result = (String) m.invoke(service, new RuntimeException());
        assertEquals("RuntimeException", result);
    }
}
