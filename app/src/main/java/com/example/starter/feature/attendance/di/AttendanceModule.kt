package com.example.starter.feature.attendance.di

import com.example.starter.core.network.ConnectivityNetworkMonitor
import com.example.starter.core.network.NetworkMonitor
import com.example.starter.feature.attendance.data.AttendanceRepositoryImpl
import com.example.starter.feature.attendance.data.FakeErpServer
import com.example.starter.feature.attendance.data.RosterRepositoryImpl
import com.example.starter.feature.attendance.data.sync.AttendanceStore
import com.example.starter.feature.attendance.data.sync.RoomAttendanceStore
import com.example.starter.feature.attendance.data.sync.SyncScheduler
import com.example.starter.feature.attendance.data.sync.WorkManagerSyncScheduler
import com.example.starter.feature.attendance.domain.AttendanceRepository
import com.example.starter.feature.attendance.domain.RosterRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AttendanceModule {
    @Binds abstract fun bindRosterRepository(impl: RosterRepositoryImpl): RosterRepository

    @Binds abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository

    @Binds abstract fun bindAttendanceStore(impl: RoomAttendanceStore): AttendanceStore

    @Binds abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    @Binds abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    companion object {
        @Provides
        @Singleton
        fun provideFakeErpServer(): FakeErpServer = FakeErpServer()
    }
}
