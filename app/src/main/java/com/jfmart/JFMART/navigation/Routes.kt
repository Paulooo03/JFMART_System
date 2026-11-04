package com.jfmart.JFMART.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("sign_up")

    object ClientHome : Screen("client_home")
    object ClientCatalog : Screen("client_catalog")
    object ClientCart : Screen("client_cart")
    object ClientOrders : Screen("client_orders")
    object ClientOrderDetails : Screen("client_order/{orderId}") {
        fun create(orderId: Long) = "client_order/$orderId"
    }
    object ClientOrderConfirmation : Screen("client_order_confirmation/{orderId}") {
        fun create(orderId: Long) = "client_order_confirmation/$orderId"
    }
    object ClientAccount : Screen("client_account")
    object ClientStatement : Screen("client_statement")

    object CourierDashboard : Screen("courier_dashboard")
    object CourierDeliveryDetails : Screen("courier_delivery_details")
    object CourierProfile : Screen("courier_profile")

    object AdminNotice : Screen("admin_notice")
}
