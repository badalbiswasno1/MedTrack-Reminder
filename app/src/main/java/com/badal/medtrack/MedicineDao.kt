package com.badal.medtrack

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {

    @Query("SELECT * FROM medicine ORDER BY name ASC")
    fun getAll(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicine WHERE id = :id")
    suspend fun getById(id: Long): Medicine?

    @Query("SELECT * FROM medicine WHERE quantity <= lowStockThreshold")
    suspend fun getLowStockMedicines(): List<Medicine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicine: Medicine): Long

    @Update
    suspend fun update(medicine: Medicine)

    @Delete
    suspend fun delete(medicine: Medicine)

    @Query("UPDATE medicine SET quantity = quantity - 1 WHERE id = :id AND quantity > 0")
    suspend fun decrementQuantity(id: Long)

    @Query("UPDATE medicine SET quantity = :newQuantity WHERE id = :id")
    suspend fun setQuantity(id: Long, newQuantity: Int)
}
