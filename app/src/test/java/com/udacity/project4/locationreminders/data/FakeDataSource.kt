package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

const val TEST_ERROR = "Test error"
const val DATA_NOT_FOUND_ERROR = "Data not found"
//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    val remindersData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    var shouldReturnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError)
            return Result.Error(TEST_ERROR)
        return Result.Success(remindersData.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersData[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError)
            return Result.Error(TEST_ERROR)
        remindersData[id]?.let {
            return Result.Success(it)
        }
        return Result.Error(DATA_NOT_FOUND_ERROR)
    }

    override suspend fun deleteAllReminders() {
        remindersData.clear()
    }

    override suspend fun deleteReminder(id: String) {
        remindersData.remove(id)
    }

    fun addData(vararg reminders: ReminderDTO) {
        reminders.forEach {
            remindersData[it.id] = it
        }
    }

}