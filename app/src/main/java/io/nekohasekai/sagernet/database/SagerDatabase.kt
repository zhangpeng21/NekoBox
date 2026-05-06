package io.nekohasekai.sagernet.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.matrix.roomigrant.GenerateRoomMigrations
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.fmt.KryoConverters
import io.nekohasekai.sagernet.fmt.gson.GsonConverters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@Database(
    entities = [ProxyGroup::class, ProxyEntity::class, RuleEntity::class],
    version = 6,
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6)
    ]
)
@TypeConverters(value = [KryoConverters::class, GsonConverters::class])
@GenerateRoomMigrations
abstract class SagerDatabase : RoomDatabase() {

    companion object {
        val instance by lazy {
            SagerNet.application.getDatabasePath(Key.DB_PROFILE).parentFile?.mkdirs()
            Room.databaseBuilder(SagerNet.application, SagerDatabase::class.java, Key.DB_PROFILE)
                // Deep-Opt: WAL (Write-Ahead Logging) instead of TRUNCATE journal mode.
                // WAL allows concurrent readers + one writer without blocking, and write
                // performance is significantly better for the frequent traffic-stat updates.
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                // Deep-Opt: removed allowMainThreadQueries(). Forcing all DB access off the
                // main thread prevents UI jank. Callers that need sync access should use
                // runBlocking{} explicitly so the cost is visible at the call site.
                // Deep-Opt: replaced GlobalScope.launch { } with Dispatchers.IO.asExecutor().
                // The old executor wrapped every query in a new coroutine (coroutine object
                // allocation + dispatcher hop) when a plain IO-thread executor is sufficient.
                .setQueryExecutor(Dispatchers.IO.asExecutor())
                .enableMultiInstanceInvalidation()
                .fallbackToDestructiveMigration()
                .build()
        }

        val groupDao get() = instance.groupDao()
        val proxyDao get() = instance.proxyDao()
        val rulesDao get() = instance.rulesDao()
    }

    abstract fun groupDao(): ProxyGroup.Dao
    abstract fun proxyDao(): ProxyEntity.Dao
    abstract fun rulesDao(): RuleEntity.Dao

}
