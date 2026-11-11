package dev.grubfx.healthconnect.queue

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The database that holds the payloads the server did not accept.
 *
 * The queue is the durable half of the export. The retry covers a
 * transport failure while the app runs, but a phone that is out of reach
 * for days would lose the days it could not send. The queue keeps them
 * until the server accepts them.
 */
@Database(entities = [QueuedPayload::class], version = 2, exportSchema = false)
abstract class PayloadQueueDatabase : RoomDatabase() {
    abstract fun payloadQueueDao(): PayloadQueueDao

    companion object {
        private const val DATABASE_NAME = "payload_queue"

        /**
         * Adds the column that marks a day as accepted by the server.
         *
         * The existing rows are not delivered, because they are the days
         * the server did not accept yet.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE queued_payloads " +
                        "ADD COLUMN delivered INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

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
                ).addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
        }
    }
}
