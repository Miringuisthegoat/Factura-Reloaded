package com.benjamin.factura.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.benjamin.factura.data.local.entities.ClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getClientById(id: String): ClientEntity?

    @Query("SELECT * FROM clients WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchClients(query: String): Flow<List<ClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Query("UPDATE clients SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAtMillis = :timestamp WHERE id = :id")
    suspend fun softDeleteClient(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM clients WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedClients(): List<ClientEntity>
}
