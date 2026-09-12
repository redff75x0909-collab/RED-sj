package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CallLogItem
import com.example.data.model.SmsLogItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLogItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(item: CallLogItem): Long

    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC")
    fun getAllSmsLogs(): Flow<List<SmsLogItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsLog(item: SmsLogItem): Long

    @Query("UPDATE sms_logs SET autoReplied = :autoReplied, replySent = :replySent WHERE id = :id")
    suspend fun updateSmsReply(id: Long, autoReplied: Boolean, replySent: String)

    @Query("DELETE FROM call_logs")
    suspend fun clearCallLogs()

    @Query("DELETE FROM sms_logs")
    suspend fun clearSmsLogs()
}
