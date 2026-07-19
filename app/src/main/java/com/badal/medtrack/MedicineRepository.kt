package com.badal.medtrack

import android.content.Context
import kotlinx.coroutines.flow.Flow

class MedicineRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).medicineDao()

    fun getAll(): Flow<List<Medicine>> = dao.getAll()

    suspend fun getAllList(): List<Medicine> = dao.getAllList()

    suspend fun getById(id: Long): Medicine? = dao.getById(id)

    suspend fun insert(medicine: Medicine): Long = dao.insert(medicine)

    suspend fun update(medicine: Medicine) = dao.update(medicine)

    suspend fun delete(medicine: Medicine) = dao.delete(medicine)

    suspend fun markDoseTaken(id: Long) = dao.decrementQuantity(id)

    suspend fun setQuantity(id: Long, quantity: Int) = dao.setQuantity(id, quantity)

    suspend fun getLowStockMedicines(): List<Medicine> = dao.getLowStockMedicines()
}
