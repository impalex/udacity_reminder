package com.udacity.project4.util

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

const val TEST_ERROR = "Test error"
const val REMINDER_NOT_FOUND_ERROR = "Reminder not found!"

class FakeAndroidTestRepository : ReminderDataSource {

    val data = LinkedHashMap<String, ReminderDTO>()

    var shouldReturnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError)
            return Result.Error(TEST_ERROR)
        return Result.Success(data.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        data[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError)
            return Result.Error("TEST_ERROR")
        return Result.Success(data[id] ?: return Result.Error(REMINDER_NOT_FOUND_ERROR))
    }

    override suspend fun deleteAllReminders() {
        data.clear()
    }

    override suspend fun deleteReminder(id: String) {
        data.remove(id)
    }
}