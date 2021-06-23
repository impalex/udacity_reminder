package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun cleanUp() = database.close()

    @Test
    fun saveReminder_loadReminder() = runBlocking {
        val reminder = ReminderDTO("title", "description", "location", 10.0, 20.0, 30f, "test_id")
        repository.saveReminder(reminder)
        val result = repository.getReminder(reminder.id)
        assertThat(result, instanceOf(Result.Success::class.java))
        result as Result.Success
        assertThat(result.data, `is`(reminder))
    }

    @Test
    fun getReminder_notFound() = runBlocking {
        val result = repository.getReminder("unknown")
        assertThat(result, instanceOf(Result.Error::class.java))
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }

    @Test
    fun saveReminders_loadRemindersList() = runBlocking {
        val data = arrayListOf(
            ReminderDTO("title1", "description1", "location1", 102.0, 112.0, 122f, "id1"),
            ReminderDTO("title2", "description2", "location2", 103.0, 113.0, 123f, "id2"),
            ReminderDTO("title3", "description3", "location3", 104.0, 114.0, 124f, "id3")
        )
        data.forEach { repository.saveReminder(it) }
        val result = repository.getReminders()
        assertThat(result, instanceOf(Result.Success::class.java))
        result as Result.Success
        result.data.forEach { reminder ->
            assertThat(reminder, `is`(data.first { it.id == reminder.id }))
        }
    }

    @Test
    fun deleteAllReminders_test() = runBlocking {
        val data = arrayListOf(
            ReminderDTO("title1", "description1", "location1", 102.0, 112.0, 122f, "id1"),
            ReminderDTO("title2", "description2", "location2", 103.0, 113.0, 123f, "id2"),
            ReminderDTO("title3", "description3", "location3", 104.0, 114.0, 124f, "id3")
        )
        data.forEach { repository.saveReminder(it) }
        repository.deleteAllReminders()
        val result = repository.getReminders()
        assertThat(result, instanceOf(Result.Success::class.java))
        result as Result.Success
        assertThat(result.data.isEmpty(), `is`(true))
    }

    @Test
    fun deleteReminder_test() = runBlocking {
        val data = ReminderDTO("title", "description", "location", 100.0, 120.0, 123f, "test_id")
        repository.saveReminder(data)
        repository.deleteReminder(data.id)
        val result = repository.getReminders()
        assertThat(result, instanceOf(Result.Success::class.java))
        result as Result.Success
        assertThat(result.data.isEmpty(), `is`(true))
    }


}