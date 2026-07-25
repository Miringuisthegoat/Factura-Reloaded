package com.benjamin.factura.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.benjamin.factura.data.local.entities.BusinessProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profiles")
    fun getAllBusinessProfiles(): Flow<List<BusinessProfileEntity>>

    @Query("SELECT * FROM business_profiles WHERE id = :id LIMIT 1")
    fun getBusinessProfileById(id: String): Flow<BusinessProfileEntity?>

    @Query("SELECT * FROM business_profiles WHERE id = :id LIMIT 1")
    suspend fun getBusinessProfileByIdOnce(id: String): BusinessProfileEntity?

    @Query("SELECT * FROM business_profiles LIMIT 1")
    fun getBusinessProfile(): Flow<BusinessProfileEntity?>

    @Query("SELECT * FROM business_profiles LIMIT 1")
    suspend fun getBusinessProfileOnce(): BusinessProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: BusinessProfileEntity)

    @Query("DELETE FROM business_profiles WHERE id = :id")
    suspend fun deleteBusinessProfile(id: String)
}
