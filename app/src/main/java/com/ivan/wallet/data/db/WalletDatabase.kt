package com.ivan.wallet.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Callback
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["externalId"], unique = true),
        Index(value = ["occurredAt"]),
        Index(value = ["normalizedMerchant"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val sender: String,
    val rawBody: String,
    val amountMinor: Long,
    val currency: String,
    val merchantDisplay: String,
    val normalizedMerchant: String,
    val occurredAt: Long,
    val balanceMinor: Long?,
    val balanceCurrency: String?,
    val receivedAt: Long,
    val categoryId: String? = null,
    val notes: String? = null
)

@Entity(
    tableName = "category_rules",
    indices = [Index(value = ["matchKey"], unique = true)]
)
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchKey: String,
    val categoryId: String,
    val updatedAt: Long
)

@Entity(tableName = "supported_currencies")
data class SupportedCurrencyEntity(
    @PrimaryKey val code: String
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId", "currency"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: String,
    val currency: String,
    val monthlyLimitMinor: Long,
    /**
     * Epoch millis. Running balance accumulates over calendar months starting from
     * the month containing this timestamp. 0 means "use earliest available tx".
     */
    val startedAt: Long = 0L
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: TransactionEntity): Long

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM transactions
            WHERE sender = :sender
              AND amountMinor = :amountMinor
              AND currency = :currency
              AND normalizedMerchant = :normalizedMerchant
              AND occurredAt = :occurredAt
        )
        """
    )
    suspend fun existsImportedEquivalent(
        sender: String,
        amountMinor: Long,
        currency: String,
        normalizedMerchant: String,
        occurredAt: Long
    ): Boolean

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id = :transactionId")
    suspend fun updateCategory(transactionId: Long, categoryId: String?)

    @Query(
        """
        UPDATE transactions
        SET merchantDisplay = :merchantDisplay,
            normalizedMerchant = :normalizedMerchant,
            amountMinor = :amountMinor,
            currency = :currency,
            occurredAt = :occurredAt,
            categoryId = :categoryId
        WHERE id = :id
        """
    )
    suspend fun updateEntry(
        id: Long,
        merchantDisplay: String,
        normalizedMerchant: String,
        amountMinor: Long,
        currency: String,
        occurredAt: Long,
        categoryId: String?
    )

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id IN (:ids)")
    suspend fun updateCategoryForIds(ids: List<Long>, categoryId: String?)

    @Query(
        """
        UPDATE transactions
        SET categoryId = :categoryId
        WHERE UPPER(normalizedMerchant) = UPPER(:key)
           OR (LENGTH(:key) >= 3 AND UPPER(normalizedMerchant) LIKE '%' || UPPER(:key) || '%')
           OR (LENGTH(normalizedMerchant) >= 3 AND UPPER(:key) LIKE '%' || UPPER(normalizedMerchant) || '%')
        """
    )
    suspend fun applyCategoryByMerchantMatch(
        key: String,
        categoryId: String?
    )
}

@Dao
interface CategoryRuleDao {
    @Query("SELECT * FROM category_rules ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM category_rules")
    suspend fun getAll(): List<CategoryRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: CategoryRuleEntity): Long

    @Query("DELETE FROM category_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SupportedCurrencyDao {
    @Query("SELECT * FROM supported_currencies ORDER BY code ASC")
    fun observeAll(): Flow<List<SupportedCurrencyEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: SupportedCurrencyEntity): Long

    @Query("DELETE FROM supported_currencies WHERE code = :code")
    suspend fun deleteByCode(code: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND currency = :currency LIMIT 1")
    suspend fun findByCategoryAndCurrency(categoryId: String, currency: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId AND currency = :currency")
    suspend fun deleteByCategoryAndCurrency(categoryId: String, currency: String)
}

@Database(
    entities = [
        TransactionEntity::class,
        CategoryRuleEntity::class,
        SupportedCurrencyEntity::class,
        BudgetEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun supportedCurrencyDao(): SupportedCurrencyDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        private val defaultCurrencies = listOf("EUR", "JPY", "RSD", "USD")

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `supported_currencies` (
                        `code` TEXT NOT NULL,
                        PRIMARY KEY(`code`)
                    )
                    """.trimIndent()
                )
                defaultCurrencies.forEach { code ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO `supported_currencies` (`code`) VALUES ('$code')"
                    )
                }
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `monthlyLimitMinor` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId_currency` ON `budgets` (`categoryId`, `currency`)"
                )
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `notes` TEXT DEFAULT NULL")
            }
        }

        private val categoryRenames = listOf(
            "DINING" to "RESTAURANTS",
            "COFFEE" to "CAFES",
            "TRANSPORT" to "PUBLIC_TRANSPORT",
            "FUEL" to "FUEL_CHARGING",
            "BILLS" to "UTILITIES",
            "SHOPPING" to "OTHER_SHOPPING",
            "TRAVEL" to "HOTELS",
            "HEALTH" to "HEALTH_PHARMACY",
            "ENTERTAINMENT" to "EVENTS_CULTURE",
            "CASH" to "OTHERS",
            "OTHER" to "OTHERS"
        )

        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                categoryRenames.forEach { (from, to) ->
                    db.execSQL(
                        "UPDATE transactions SET categoryId = ? WHERE categoryId = ?",
                        arrayOf<Any>(to, from)
                    )
                    db.execSQL(
                        "UPDATE category_rules SET categoryId = ? WHERE categoryId = ?",
                        arrayOf<Any>(to, from)
                    )
                    // Budgets have a unique index on (categoryId, currency); avoid conflicts
                    // by deleting any old-named row that would collide with an already-renamed one.
                    db.execSQL(
                        """
                        DELETE FROM budgets
                        WHERE categoryId = ?
                          AND EXISTS (
                              SELECT 1 FROM budgets b2
                              WHERE b2.categoryId = ? AND b2.currency = budgets.currency
                          )
                        """.trimIndent(),
                        arrayOf<Any>(from, to)
                    )
                    db.execSQL(
                        "UPDATE budgets SET categoryId = ? WHERE categoryId = ?",
                        arrayOf<Any>(to, from)
                    )
                }
            }
        }

        private val migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budgets ADD COLUMN startedAt INTEGER NOT NULL DEFAULT 0")
                // Backfill startedAt to the earliest matching expense transaction's
                // occurredAt; fall back to current time if there's no history.
                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    UPDATE budgets
                    SET startedAt = COALESCE(
                        (SELECT MIN(occurredAt) FROM transactions
                         WHERE transactions.categoryId = budgets.categoryId
                           AND transactions.currency = budgets.currency
                           AND transactions.amountMinor > 0),
                        ?
                    )
                    """.trimIndent(),
                    arrayOf<Any>(now)
                )
            }
        }

        fun create(context: Context): WalletDatabase {
            return Room.databaseBuilder(
                context,
                WalletDatabase::class.java,
                "wallet.db"
            )
                .addMigrations(migration1To2, migration2To3, migration3To4, migration4To5)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        defaultCurrencies.forEach { code ->
                            db.execSQL(
                                "INSERT OR IGNORE INTO `supported_currencies` (`code`) VALUES ('$code')"
                            )
                        }
                    }
                })
                .build()
        }
    }
}
