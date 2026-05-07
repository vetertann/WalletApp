package com.ivan.wallet

import android.app.Application
import android.content.Context
import com.ivan.wallet.data.db.WalletDatabase
import com.ivan.wallet.data.parser.AltaBankSmsParser
import com.ivan.wallet.data.repository.TransactionRepository
import com.ivan.wallet.data.sms.SmsImporter

class WalletApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

    companion object {
        fun container(context: Context): AppContainer {
            return (context.applicationContext as WalletApplication).appContainer
        }
    }
}

class AppContainer(context: Context) {
    private val database = WalletDatabase.create(context)
    private val parser = AltaBankSmsParser()
    private val repository = TransactionRepository(
        transactionDao = database.transactionDao(),
        ruleDao = database.categoryRuleDao(),
        supportedCurrencyDao = database.supportedCurrencyDao(),
        budgetDao = database.budgetDao()
    )

    val transactionRepository: TransactionRepository = repository
    val smsImporter: SmsImporter = SmsImporter(repository = repository, parser = parser)
    val bankParser: AltaBankSmsParser = parser
}
