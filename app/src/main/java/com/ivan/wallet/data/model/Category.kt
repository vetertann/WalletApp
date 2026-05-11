package com.ivan.wallet.data.model

import androidx.compose.ui.graphics.Color

enum class CategoryGroup(val label: String, val accent: Color) {
    FOOD_DRINKS("Food & Drinks", Color(0xFFD84315)),
    SHOPPING("Shopping", Color(0xFF6A1B9A)),
    HOUSING("Housing", Color(0xFF5D4037)),
    TRANSPORTATION("Transportation", Color(0xFF1565C0)),
    VEHICLE("Vehicle", Color(0xFF455A64)),
    LIFE_ENTERTAINMENT("Life & Entertainment", Color(0xFFF9A825)),
    COMMUNICATION_PC("Communication, PC", Color(0xFF00838F)),
    FINANCIAL("Financial expenses", Color(0xFF37474F)),
    INVESTMENTS("Investments", Color(0xFF2E7D32)),
    INCOME("Income", Color(0xFF1B5E20)),
    OTHERS("Others", Color(0xFF616161))
}

enum class Category(
    val label: String,
    val accent: Color,
    val group: CategoryGroup
) {
    // Food & Drinks
    GROCERIES("Groceries", Color(0xFF66BB6A), CategoryGroup.FOOD_DRINKS),
    RESTAURANTS("Restaurants", Color(0xFFEF5350), CategoryGroup.FOOD_DRINKS),
    CAFES("Cafés", Color(0xFF8D6E63), CategoryGroup.FOOD_DRINKS),
    FOOD_DELIVERY("Food delivery", Color(0xFFFF7043), CategoryGroup.FOOD_DRINKS),
    WORK_LUNCH("Work lunch", Color(0xFFA1887F), CategoryGroup.FOOD_DRINKS),
    EVENTS_CATERING("Events / catering", Color(0xFFFFA726), CategoryGroup.FOOD_DRINKS),

    // Shopping
    CLOTHES_SHOES("Clothes & shoes", Color(0xFFAB47BC), CategoryGroup.SHOPPING),
    ELECTRONICS("Electronics", Color(0xFF7E57C2), CategoryGroup.SHOPPING),
    HOME_GOODS("Home goods", Color(0xFF5C6BC0), CategoryGroup.SHOPPING),
    GIFTS("Gifts", Color(0xFFEC407A), CategoryGroup.SHOPPING),
    OTHER_SHOPPING("Other shopping", Color(0xFF8E24AA), CategoryGroup.SHOPPING),

    // Housing
    RENT_MORTGAGE("Rent / mortgage", Color(0xFF6D4C41), CategoryGroup.HOUSING),
    UTILITIES("Utilities", Color(0xFF8D6E63), CategoryGroup.HOUSING),
    HOME_REPAIRS("Home repairs", Color(0xFFA1887F), CategoryGroup.HOUSING),
    FURNITURE_APPLIANCES("Furniture & appliances", Color(0xFFBCAAA4), CategoryGroup.HOUSING),
    BUILDING_FEES("Building fees", Color(0xFF795548), CategoryGroup.HOUSING),
    HOME_INSURANCE("Home insurance", Color(0xFF607D8B), CategoryGroup.HOUSING),

    // Transportation
    PUBLIC_TRANSPORT("Public transport", Color(0xFF1976D2), CategoryGroup.TRANSPORTATION),
    TAXI("Taxi / ride-hailing", Color(0xFFFFCA28), CategoryGroup.TRANSPORTATION),
    INTERCITY("Intercity transport", Color(0xFF42A5F5), CategoryGroup.TRANSPORTATION),
    FLIGHTS("Flights", Color(0xFF3949AB), CategoryGroup.TRANSPORTATION),
    PARKING("Parking", Color(0xFF546E7A), CategoryGroup.TRANSPORTATION),
    BIKE_SCOOTER("Bike / scooter rental", Color(0xFF26A69A), CategoryGroup.TRANSPORTATION),

    // Vehicle
    FUEL_CHARGING("Fuel / charging", Color(0xFF455A64), CategoryGroup.VEHICLE),
    VEHICLE_MAINTENANCE("Vehicle maintenance", Color(0xFF607D8B), CategoryGroup.VEHICLE),
    VEHICLE_INSURANCE("Vehicle insurance", Color(0xFF78909C), CategoryGroup.VEHICLE),
    VEHICLE_REGISTRATION("Vehicle registration", Color(0xFF90A4AE), CategoryGroup.VEHICLE),
    CAR_WASH("Car wash", Color(0xFF4FC3F7), CategoryGroup.VEHICLE),
    FINES("Fines", Color(0xFFE53935), CategoryGroup.VEHICLE),

    // Life & Entertainment
    SUBSCRIPTIONS("Subscriptions", Color(0xFF00838F), CategoryGroup.LIFE_ENTERTAINMENT),
    EVENTS_CULTURE("Events & culture", Color(0xFFFFB300), CategoryGroup.LIFE_ENTERTAINMENT),
    SPORTS_GYM("Sports & gym", Color(0xFF26C6DA), CategoryGroup.LIFE_ENTERTAINMENT),
    HOTELS("Hotels", Color(0xFFD81B60), CategoryGroup.LIFE_ENTERTAINMENT),
    HEALTH_PHARMACY("Health & pharmacy", Color(0xFFC62828), CategoryGroup.LIFE_ENTERTAINMENT),
    EDUCATION("Education", Color(0xFF5E35B1), CategoryGroup.LIFE_ENTERTAINMENT),
    BEAUTY("Beauty", Color(0xFFE91E63), CategoryGroup.LIFE_ENTERTAINMENT),

    // Communication, PC
    MOBILE("Mobile", Color(0xFF0288D1), CategoryGroup.COMMUNICATION_PC),
    INTERNET("Internet", Color(0xFF039BE5), CategoryGroup.COMMUNICATION_PC),
    SOFTWARE_APPS("Software & apps", Color(0xFF00ACC1), CategoryGroup.COMMUNICATION_PC),
    HARDWARE_ACCESSORIES("Hardware & accessories", Color(0xFF00897B), CategoryGroup.COMMUNICATION_PC),
    HOSTING_CLOUD("Hosting & cloud", Color(0xFF43A047), CategoryGroup.COMMUNICATION_PC),
    PC_REPAIRS("Tech repairs", Color(0xFF7CB342), CategoryGroup.COMMUNICATION_PC),

    // Financial expenses
    BANK_FEES("Bank fees", Color(0xFF455A64), CategoryGroup.FINANCIAL),
    LOAN_PAYMENTS("Loan payments", Color(0xFF6D4C41), CategoryGroup.FINANCIAL),
    TAXES("Taxes", Color(0xFFB71C1C), CategoryGroup.FINANCIAL),
    LEGAL_ACCOUNTING("Legal & accounting", Color(0xFF37474F), CategoryGroup.FINANCIAL),
    CURRENCY_EXCHANGE("Currency exchange", Color(0xFF558B2F), CategoryGroup.FINANCIAL),
    PAYMENT_COMMISSIONS("Payment commissions", Color(0xFFE53935), CategoryGroup.FINANCIAL),

    // Investments
    STOCKS_ETFS("Stocks & ETFs", Color(0xFF1B5E20), CategoryGroup.INVESTMENTS),
    CRYPTO("Crypto", Color(0xFFF57F17), CategoryGroup.INVESTMENTS),
    REAL_ESTATE("Real estate", Color(0xFF4E342E), CategoryGroup.INVESTMENTS),
    DEPOSITS_SAVINGS("Deposits & savings", Color(0xFF00695C), CategoryGroup.INVESTMENTS),
    PENSION("Pension", Color(0xFF283593), CategoryGroup.INVESTMENTS),
    BROKERAGE_FEES("Brokerage fees", Color(0xFF6D4C41), CategoryGroup.INVESTMENTS),

    // Income
    SALARY("Salary", Color(0xFF2E7D32), CategoryGroup.INCOME),
    BUSINESS_INCOME("Business income", Color(0xFF388E3C), CategoryGroup.INCOME),
    FREELANCE_INCOME("Freelance income", Color(0xFF43A047), CategoryGroup.INCOME),
    INVESTMENT_INCOME("Investment income", Color(0xFF66BB6A), CategoryGroup.INCOME),
    RENTAL_INCOME("Rental income", Color(0xFF81C784), CategoryGroup.INCOME),
    REFUNDS_CASHBACK("Refunds & cashback", Color(0xFFA5D6A7), CategoryGroup.INCOME),

    // Others
    OTHERS("Others", Color(0xFF616161), CategoryGroup.OTHERS);

    val isIncome: Boolean get() = group == CategoryGroup.INCOME

    companion object {
        private val legacyAliases by lazy {
            mapOf(
                "DINING" to RESTAURANTS,
                "COFFEE" to CAFES,
                "TRANSPORT" to PUBLIC_TRANSPORT,
                "FUEL" to FUEL_CHARGING,
                "BILLS" to UTILITIES,
                "SHOPPING" to OTHER_SHOPPING,
                "TRAVEL" to HOTELS,
                "HEALTH" to HEALTH_PHARMACY,
                "ENTERTAINMENT" to EVENTS_CULTURE,
                "CASH" to OTHERS,
                "OTHER" to OTHERS
            )
        }

        fun fromId(id: String?): Category? {
            if (id == null) return null
            return entries.firstOrNull { it.name == id } ?: legacyAliases[id]
        }
    }
}
