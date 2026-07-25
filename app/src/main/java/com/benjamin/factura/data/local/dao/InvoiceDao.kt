package com.benjamin.factura.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.benjamin.factura.data.local.entities.InvoiceEntity
import com.benjamin.factura.data.model.InvoiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE isDeleted = 0 ORDER BY issueDateMillis DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getInvoiceById(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE clientId = :clientId AND isDeleted = 0 ORDER BY issueDateMillis DESC")
    fun getInvoicesForClient(clientId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE clientId = :clientId AND isDeleted = 0 ORDER BY issueDateMillis DESC LIMIT :limit")
    suspend fun getRecentInvoicesForClientOnce(clientId: String, limit: Int = 5): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE isDeleted = 0 AND status = :status ORDER BY issueDateMillis DESC")
    fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE isDeleted = 0 AND (invoiceNumber LIKE '%' || :query || '%' OR clientName LIKE '%' || :query || '%') ORDER BY issueDateMillis DESC")
    fun searchInvoices(query: String): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("UPDATE invoices SET status = :status, amountPaid = :amountPaid, balanceDue = :balanceDue, updatedAtMillis = :timestamp WHERE id = :id")
    suspend fun updateInvoicePaymentStatus(id: String, status: InvoiceStatus, amountPaid: Double, balanceDue: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE invoices SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAtMillis = :timestamp WHERE id = :id")
    suspend fun softDeleteInvoice(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM invoices WHERE isDeleted = 0")
    fun getInvoiceCount(): Flow<Int>

    @Query("SELECT * FROM invoices WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedInvoices(): List<InvoiceEntity>
}
