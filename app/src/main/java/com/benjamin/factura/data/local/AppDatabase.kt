package com.benjamin.factura.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.benjamin.factura.data.local.dao.BusinessProfileDao
import com.benjamin.factura.data.local.dao.ClientDao
import com.benjamin.factura.data.local.dao.ExpenseDao
import com.benjamin.factura.data.local.dao.InvoiceDao
import com.benjamin.factura.data.local.dao.PaymentDao
import com.benjamin.factura.data.local.entities.BusinessProfileEntity
import com.benjamin.factura.data.local.entities.ClientEntity
import com.benjamin.factura.data.local.entities.ExpenseEntity
import com.benjamin.factura.data.local.entities.InvoiceEntity
import com.benjamin.factura.data.local.entities.PaymentEntity

@Database(
    entities = [
        BusinessProfileEntity::class,
        ClientEntity::class,
        InvoiceEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "factura_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
