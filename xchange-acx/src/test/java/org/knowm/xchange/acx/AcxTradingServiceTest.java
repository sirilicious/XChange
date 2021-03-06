package org.knowm.xchange.acx;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.orders.DefaultOpenOrdersParamCurrencyPair;
import org.known.xchange.acx.AcxApi;
import org.known.xchange.acx.AcxMapper;
import org.known.xchange.acx.AcxSignatureCreator;
import org.known.xchange.acx.dto.account.AcxAccountInfo;
import org.known.xchange.acx.dto.marketdata.AcxOrder;
import org.known.xchange.acx.service.account.AcxAccountService;
import org.known.xchange.acx.service.trade.AcxTradeService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.knowm.xchange.dto.Order.OrderType.BID;
import static org.mockito.Matchers.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class AcxTradingServiceTest {

    private AcxApi api;
    private ObjectMapper objectMapper;
    private TradeService service;
    private String accessKey;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        AcxMapper mapper = new AcxMapper();
        api = mock(AcxApi.class);
        accessKey = "access_key";
        service = new AcxTradeService(api, mapper, mock(AcxSignatureCreator.class), accessKey);
    }

    @Test
    public void testGetOrders() throws IOException {
        when(api.getOrders(eq(accessKey), anyLong(), any(), any()))
                .thenReturn(read("/trade/open_orders.json", new TypeReference<List<AcxOrder>>() {}));

        List<LimitOrder> openOrders = service.getOpenOrders(new DefaultOpenOrdersParamCurrencyPair(CurrencyPair.ETH_AUD))
                .getOpenOrders();

        assertEquals(1, openOrders.size());
        assertEquals("198602763", openOrders.get(0).getId());
        assertEquals(new BigDecimal("97900.99"), openOrders.get(0).getLimitPrice());
        assertEquals(new BigDecimal("0.01"), openOrders.get(0).getRemainingAmount());
        assertEquals(new BigDecimal("0.01"), openOrders.get(0).getOriginalAmount());
        assertEquals(new BigDecimal("0.00"), openOrders.get(0).getCumulativeAmount());
    }

    @Test
    public void testCreateOrder() throws IOException {
        when(api.createOrder(eq(accessKey), anyLong(), eq("btcaud"), eq("buy"), any(), any(), any(), any()))
                .thenReturn(read("/trade/create_order.json", AcxOrder.class));

        LimitOrder order = new LimitOrder.Builder(Order.OrderType.BID, CurrencyPair.BTC_AUD)
                .limitPrice(new BigDecimal(10.1234))
                .originalAmount(new BigDecimal(0.1))
                .build();
        String id = service.placeLimitOrder(order);

        assertEquals("199918493", id);
    }


    @Test
    public void testCancelOrder() throws IOException {
        String orderId = "198602763";
        when(api.cancelOrder(eq(accessKey), anyLong(), eq(orderId), any()))
                .thenReturn(read("/trade/cancel_order.json", AcxOrder.class));

        boolean result = service.cancelOrder(orderId);

        assertEquals(true, result);
    }


    private <T> T read(String path, Class<T> clz) throws IOException {
        return objectMapper.readValue(this.getClass().getResourceAsStream(path), clz);
    }

    private <T> T read(String path, TypeReference<T> type) throws IOException {
        return objectMapper.readValue(this.getClass().getResourceAsStream(path), type);
    }
}
