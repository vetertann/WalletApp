package com.ivan.wallet.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.LocalCarWash
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Phishing
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.ivan.wallet.data.model.Category

fun iconForCategory(category: Category?, isManual: Boolean): ImageVector {
    return when (category) {
        // Food & Drinks
        Category.GROCERIES -> Icons.Outlined.LocalGroceryStore
        Category.RESTAURANTS -> Icons.Outlined.Restaurant
        Category.CAFES -> Icons.Outlined.Coffee
        Category.FOOD_DELIVERY -> Icons.Outlined.DeliveryDining
        Category.WORK_LUNCH -> Icons.Outlined.LunchDining
        Category.EVENTS_CATERING -> Icons.Outlined.Cake

        // Shopping
        Category.CLOTHES_SHOES -> Icons.Outlined.Checkroom
        Category.ELECTRONICS -> Icons.Outlined.Devices
        Category.HOME_GOODS -> Icons.Outlined.Chair
        Category.GIFTS -> Icons.Outlined.CardGiftcard
        Category.OTHER_SHOPPING -> Icons.Outlined.ShoppingBag

        // Housing
        Category.RENT_MORTGAGE -> Icons.Outlined.House
        Category.UTILITIES -> Icons.AutoMirrored.Outlined.ReceiptLong
        Category.HOME_REPAIRS -> Icons.Outlined.Build
        Category.FURNITURE_APPLIANCES -> Icons.Outlined.Chair
        Category.BUILDING_FEES -> Icons.Outlined.Apartment
        Category.HOME_INSURANCE -> Icons.Outlined.Security

        // Transportation
        Category.PUBLIC_TRANSPORT -> Icons.Outlined.DirectionsBus
        Category.TAXI -> Icons.Outlined.LocalTaxi
        Category.INTERCITY -> Icons.Outlined.Train
        Category.FLIGHTS -> Icons.Outlined.Flight
        Category.PARKING -> Icons.Outlined.LocalParking
        Category.BIKE_SCOOTER -> Icons.AutoMirrored.Outlined.DirectionsBike

        // Vehicle
        Category.FUEL_CHARGING -> Icons.Outlined.LocalGasStation
        Category.VEHICLE_MAINTENANCE -> Icons.Outlined.Build
        Category.VEHICLE_INSURANCE -> Icons.Outlined.Security
        Category.VEHICLE_REGISTRATION -> Icons.AutoMirrored.Outlined.ReceiptLong
        Category.CAR_WASH -> Icons.Outlined.LocalCarWash
        Category.FINES -> Icons.Outlined.Warning

        // Life & Entertainment
        Category.SUBSCRIPTIONS -> Icons.Outlined.Subscriptions
        Category.EVENTS_CULTURE -> Icons.Outlined.TheaterComedy
        Category.SPORTS_GYM -> Icons.Outlined.FitnessCenter
        Category.HOTELS -> Icons.Outlined.Hotel
        Category.HEALTH_PHARMACY -> Icons.Outlined.LocalPharmacy
        Category.EDUCATION -> Icons.Outlined.School
        Category.BEAUTY -> Icons.Outlined.Brush

        // Communication, PC
        Category.MOBILE -> Icons.Outlined.PhoneIphone
        Category.INTERNET -> Icons.Outlined.Wifi
        Category.SOFTWARE_APPS -> Icons.Outlined.Apps
        Category.HARDWARE_ACCESSORIES -> Icons.Outlined.Memory
        Category.HOSTING_CLOUD -> Icons.Outlined.Cloud
        Category.PC_REPAIRS -> Icons.Outlined.Build

        // Financial expenses
        Category.BANK_FEES -> Icons.Outlined.AttachMoney
        Category.LOAN_PAYMENTS -> Icons.Outlined.RequestQuote
        Category.TAXES -> Icons.AutoMirrored.Outlined.ReceiptLong
        Category.LEGAL_ACCOUNTING -> Icons.Outlined.Gavel
        Category.CURRENCY_EXCHANGE -> Icons.Outlined.CurrencyExchange
        Category.PAYMENT_COMMISSIONS -> Icons.Outlined.Percent

        // Investments
        Category.STOCKS_ETFS -> Icons.AutoMirrored.Outlined.ShowChart
        Category.CRYPTO -> Icons.Outlined.CurrencyBitcoin
        Category.REAL_ESTATE -> Icons.Outlined.House
        Category.DEPOSITS_SAVINGS -> Icons.Outlined.Savings
        Category.PENSION -> Icons.Outlined.Phishing
        Category.BROKERAGE_FEES -> Icons.Outlined.Percent

        // Income
        Category.SALARY -> Icons.Outlined.Payments
        Category.BUSINESS_INCOME -> Icons.Outlined.Business
        Category.FREELANCE_INCOME -> Icons.Outlined.Computer
        Category.INVESTMENT_INCOME -> Icons.AutoMirrored.Outlined.ShowChart
        Category.RENTAL_INCOME -> Icons.Outlined.House
        Category.REFUNDS_CASHBACK -> Icons.Outlined.CurrencyExchange

        // Others
        Category.OTHERS -> Icons.Outlined.Wallet

        null -> if (isManual) Icons.Outlined.Add else Icons.Outlined.Fastfood
    }
}

