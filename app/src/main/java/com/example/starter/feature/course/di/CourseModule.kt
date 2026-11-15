package com.example.starter.feature.course.di

import com.example.starter.core.network.ConnectivityNetworkMonitor
import com.example.starter.core.network.NetworkMonitor
import com.example.starter.feature.course.data.CourseRepositoryImpl
import com.example.starter.feature.course.data.FakeCourseServer
import com.example.starter.feature.course.data.download.AppFilesLessonStore
import com.example.starter.feature.course.data.download.DownloadRepositoryImpl
import com.example.starter.feature.course.data.download.DownloadScheduler
import com.example.starter.feature.course.data.download.DownloadStore
import com.example.starter.feature.course.data.download.FakeLessonDownloader
import com.example.starter.feature.course.data.download.LessonDownloader
import com.example.starter.feature.course.data.download.LessonFileStore
import com.example.starter.feature.course.data.download.RoomDownloadStore
import com.example.starter.feature.course.data.download.WorkManagerDownloadScheduler
import com.example.starter.feature.course.domain.CourseRepository
import com.example.starter.feature.course.domain.DownloadRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CourseModule {
    @Binds abstract fun bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository

    @Binds abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds abstract fun bindDownloadStore(impl: RoomDownloadStore): DownloadStore

    @Binds abstract fun bindDownloadScheduler(impl: WorkManagerDownloadScheduler): DownloadScheduler

    @Binds abstract fun bindLessonFileStore(impl: AppFilesLessonStore): LessonFileStore

    @Binds @Singleton
    abstract fun bindLessonDownloader(impl: FakeLessonDownloader): LessonDownloader

    @Binds abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    companion object {
        @Provides
        @Singleton
        fun provideFakeCourseServer(): FakeCourseServer = FakeCourseServer()
    }
}
