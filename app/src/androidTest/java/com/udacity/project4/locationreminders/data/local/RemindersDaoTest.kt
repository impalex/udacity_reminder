package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun getReminders_empty() = runBlockingTest {
        val list = database.reminderDao().getReminders()
        assertThat(list.size, `is`(0))
    }

    @Test
    fun getReminders_insertAndGetList() = runBlockingTest {
        val data = arrayListOf(
            ReminderDTO("title1", "description1", "location1", 102.0, 112.0, 122f, "id1"),
            ReminderDTO("title2", "description2", "location2", 103.0, 113.0, 123f, "id2"),
            ReminderDTO("title3", "description3", "location3", 104.0, 114.0, 124f, "id3")
        )
        data.forEach { database.reminderDao().saveReminder(it) }
        val savedData = database.reminderDao().getReminders()
        assertThat(savedData.size, `is`(data.size))
        savedData.forEach { item ->
            assertThat(item, `is`(data.first { it.id == item.id }))
        }
    }

    @Test
    fun saveReminder_getById() = runBlockingTest {
        val data = ReminderDTO("title", "description", "location", 100.0, 200.0, 300f, "id")
        database.reminderDao().saveReminder(data)
        val saved = database.reminderDao().getReminderById(data.id)
        assertThat(saved, `is`(data))
    }

    @Test
    fun saveReminder_update() = runBlockingTest {
        val data = ReminderDTO("title1", "description1", "location1", 100.0, 200.0, 300f, "id")
        database.reminderDao().saveReminder(data)
        val newData = ReminderDTO("title2", "description2", "location2", 110.0, 220.0, 330f, data.id)
        database.reminderDao().saveReminder(newData)
        val saved = database.reminderDao().getReminderById(data.id)
        assertThat(saved, `is`(newData))
    }

    @Test
    fun deleteAllReminders_test() = runBlockingTest {
        val data = arrayListOf(
            ReminderDTO("title1", "description1", "location1", 102.0, 112.0, 122f, "id1"),
            ReminderDTO("title2", "description2", "location2", 103.0, 113.0, 123f, "id2"),
            ReminderDTO("title3", "description3", "location3", 104.0, 114.0, 124f, "id3")
        )
        data.forEach { database.reminderDao().saveReminder(it) }
        database.reminderDao().deleteAllReminders()
        val reminders = database.reminderDao().getReminders()
        assertThat(reminders.isEmpty(), `is`(true))
    }

    @Test
    fun deleteReminderById() = runBlockingTest {
        val data = ReminderDTO("title1", "description1", "location1", 100.0, 200.0, 300f, "id")
        database.reminderDao().saveReminder(data)
        database.reminderDao().deleteReminder(data.id)
        val saved = database.reminderDao().getReminderById(data.id)
        assertThat(saved, nullValue())
    }


}