package dev.grubfx.healthconnect.queue

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The database that holds the payloads the server did not accept.
 *
 * The queue is the durable half of the export. The retry covers a
 * transport failure while the app runs, but a phone that is out of reach
 * for days would lose the days it could not send. The queue keeps them
 * until the server accepts them.
 */
@Database(entities = [QueuedPayload::class], version = 1, exportSchema = false)
abstract class PayloadQueueDatabase : RoomDatabase() {
    abstract fun payloadQueueDao(): PayloadQueueDao

    companion object {
        private const val DATABASE_NAME = "payload_queue"

        @Volatile
        private var instance: PayloadQueueDatabase? = null

        /**
         * The one database of the process. Room opens the database once,
         * because opening it is expensive and the worker and the settings
         * screen use the same one.
         */
        fun get(context: Context): PayloadQueueDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PayloadQueueDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
        }
    }
}
