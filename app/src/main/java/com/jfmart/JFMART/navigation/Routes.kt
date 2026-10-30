package com.jfmart.JFMART.navigation

sealed class Screen(val route: String) {
    object ClientLogin : Screen("client_login")
    object ClientDashboard : Screen("client_dashboard")
    object ClientAvailableItems : Screen("client_available_items")
    object ClientCart : Screen("client_cart")
    object ClientManageAccount : Screen("client_manage_account")
    object ClientOrderDetails : Screen("client_order_details")
    object ClientPayments : Screen("client_payments")

    object CourierLogin : Screen("courier_login")
    object CourierDashboard : Screen("courier_dashboard")
    object CourierDeliveryDetails : Screen("courier_delivery_details")

    object CourierProfile : Screen("courier_profile")
}