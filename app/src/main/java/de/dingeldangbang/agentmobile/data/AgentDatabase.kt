package de.dingeldangbang.agentmobile.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prompt: String,
    val result: String,
    val source: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val embedding: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entity: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY createdAt DESC LIMIT :limit")
    fun observeLatest(limit: Int = 20): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DocumentEntity>)

    @Query("SELECT * FROM documents WHERE content LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY updatedAt DESC LIMIT 5")
    suspend fun search(query: String): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE embedding != ''")
    suspend fun withEmbeddings(): List<DocumentEntity>

    @Query("DELETE FROM documents")
    suspend fun clear()
}

@Database(
    entities = [HistoryEntity::class, DocumentEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun documentDao(): DocumentDao
}
