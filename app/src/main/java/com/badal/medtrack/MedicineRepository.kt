package com.badal.medtrack

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class MedicineRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).medicineDao()
    private val logDao = AppDatabase.getInstance(context).doseLogDao()
    private val healthDao = AppDatabase.getInstance(context).healthLogDao()
    private val appointmentDao = AppDatabase.getInstance(context).doctorAppointmentDao()

    fun getAll(): Flow<List<Medicine>> = dao.getAll()

    suspend fun getAllList(): List<Medicine> = dao.getAllList()

    suspend fun getById(id: Long): Medicine? = dao.getById(id)

    suspend fun insert(medicine: Medicine): Long = dao.insert(medicine)

    suspend fun update(medicine: Medicine) = dao.update(medicine)

    suspend fun delete(medicine: Medicine) = dao.delete(medicine)

    suspend fun markDoseTaken(id: Long) = dao.decrementQuantity(id)

    suspend fun setQuantity(id: Long, quantity: Int) = dao.setQuantity(id, quantity)

    suspend fun getLowStockMedicines(): List<Medicine> = dao.getLowStockMedicines()

    suspend fun createDoseLog(medicineId: Long, slotIndex: Int): Long {
        val log = DoseLog(
            medicineId = medicineId,
            slotIndex = slotIndex,
            scheduledDateTime = System.currentTimeMillis(),
            status = DoseStatus.PENDING
        )
        return logDao.insert(log)
    }

    suspend fun getPendingLog(medicineId: Long, slotIndex: Int): DoseLog? {
        val dayStart = startOfToday()
        return logDao.getPendingForSlot(medicineId, slotIndex, dayStart)
    }

    suspend fun markLogTaken(logId: Long) {
        logDao.updateStatus(logId, DoseStatus.TAKEN, System.currentTimeMillis())
    }

    suspend fun markLogSkipped(logId: Long) {
        logDao.updateStatus(logId, DoseStatus.SKIPPED, null)
    }

    suspend fun markStaleLogsMissed() {
        val cutoff = startOfToday()
        val stale = logDao.getStalePending(cutoff)
        for (log in stale) {
            logDao.updateStatus(log.id, DoseStatus.MISSED, null)
        }
    }

    suspend fun getTodayLogs(): List<DoseLog> {
        val start = startOfToday()
        val end = start + 24 * 60 * 60 * 1000
        return logDao.getLogsForDay(start, end)
    }

    suspend fun getLogsInRange(start: Long, end: Long): List<DoseLog> = logDao.getLogsInRange(start, end)

    suspend fun getAllLogsAsc(): List<DoseLog> = logDao.getAllLogsAsc()

    suspend fun getTotalTakenCount(): Int = logDao.getTotalTakenCount()

    suspend fun getTotalMissedCount(): Int = logDao.getTotalMissedCount()

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }


    suspend fun insertHealthLog(log: HealthLog): Long = healthDao.insert(log)

    suspend fun getHealthLogsByType(type: String): List<HealthLog> = healthDao.getByType(type)

    suspend fun getLatestHealthLog(type: String): HealthLog? = healthDao.getLatestByType(type)

    suspend fun getAllHealthLogs(): List<HealthLog> = healthDao.getAll()

    suspend fun deleteHealthLog(id: Long) = healthDao.deleteById(id)

    suspend fun getTodayCount(type: String): Int {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 24 * 60 * 60 * 1000
        return healthDao.getCountByTypeInRange(type, start, end)
    }


    suspend fun insertAppointment(appointment: DoctorAppointment): Long = appointmentDao.insert(appointment)

    suspend fun getAllAppointments(): List<DoctorAppointment> = appointmentDao.getAll()

    suspend fun getUpcomingAppointments(): List<DoctorAppointment> = appointmentDao.getUpcoming(System.currentTimeMillis())

    suspend fun deleteAppointment(id: Long) = appointmentDao.deleteById(id)
}