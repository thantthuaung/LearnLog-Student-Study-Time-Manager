package com.example.learnlog.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.example.learnlog.data.repository.*
import com.example.learnlog.data.dao.*
import com.example.learnlog.data.preferences.UserPreferences
import com.example.learnlog.util.DateTimeProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSubjectRepository(subjectDao: SubjectDao): SubjectRepository {
        return SubjectRepository(subjectDao)
    }

    @Provides
    @Singleton
    fun provideInsightsRepository(
        sessionLogDao: SessionLogDao,
        tasksRepository: TasksRepository,
        plannerRepository: PlannerRepository,
        dateTimeProvider: DateTimeProvider
    ): InsightsRepository {
        return InsightsRepository(sessionLogDao, tasksRepository, plannerRepository, dateTimeProvider)
    }

    @Provides
    @Singleton
    fun provideDateTimeProvider(): DateTimeProvider {
        return DateTimeProvider()
    }

    @Provides
    @Singleton
    fun provideTasksRepository(dateTimeProvider: DateTimeProvider): TasksRepository {
        return TasksRepository(dateTimeProvider)
    }

    @Provides
    @Singleton
    fun providePlannerRepository(
        tasksRepository: TasksRepository,
        dateTimeProvider: DateTimeProvider
    ): PlannerRepository {
        return PlannerRepository(tasksRepository, dateTimeProvider)
    }

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }
}
