package br.com.tp.lncr.aws.lambda;

import java.util.List;

public class TestListRoutes {

    // Array simples com combinações method,path para testes
    public static final String[][]   TEST_ROUTES = {
        {"DELETE", "/customer/{customerId}"},
        {"DELETE", "/foodItems/image/{foodItemImageId}"},
        {"DELETE", "/foodItems/{foodItemId}"},
        {"DELETE", "/foodItems/{foodItemId}/images"},
        {"GET", "/customer/documentNumber/{documentNumber}"},
        {"GET", "/customer/listIds/{customerIdList}"},
        {"GET", "/customer/{customerId}"},
        {"GET", "/customerOrder/{customerOrderId}"},
        {"GET", "/customerOrders/status/{statusList}"},
        {"GET", "/customers"},
        {"GET", "/foodItems/image/{foodItemImageId}"},
        {"GET", "/foodItems/{foodItemId}"},
        {"GET", "/foodItems/{foodItemId}/images"},
        {"GET", "/foodItems"},
        {"GET", "/kitchenOrders/customerOrder/{customerOrderId}"},
        {"GET", "/kitchenOrders/{kitchenOrderId}"},
        {"GET", "/kitchenOrders/status/{statusList}"},
        {"GET", "/kitchenOrders"},
        {"GET", "/notifications/"},
        {"GET", "/notifications/{notificationType}"},
        {"GET", "/payments/mercadoPago/{customerOrderId}/get"},
        {"PATCH", "/customer/documentNumber/{documentNumber}"},
        {"PATCH", "/customer/{customerId}"},
        {"PATCH", "/customerOrder/{customerOrderId}/updateStatus/{newStatus}"},
        {"PATCH", "/foodItems/{foodItemId}"},
        {"PATCH", "/kitchenOrders/{kitchenOrderId}/updateStatus/{newStatus}"},
        {"PATCH", "/payments/mercadoPago/paymentReceived"},
        {"PATCH", "/payments/mercadoPago/{customerOrderId}/cancel"},
        {"POST", "/customerOrders"},
        {"POST", "/customers"},
        {"POST", "/foodItems/{foodItemId}/images"},
        {"POST", "/foodItems"},
        {"POST", "/kitchenOrders"},
        {"POST", "/payments/mercadoPago/charge"},
        {"POST", "/webhooks/payments/mercadoPago/callback"},
        {"PUT", "/foodItems/image/{foodItemImageId}"}
    };

    // Lista simples para testes parametrizados
    public static List<String[]> getTestRoutes() {
        return List.of(TEST_ROUTES);
    }
}
