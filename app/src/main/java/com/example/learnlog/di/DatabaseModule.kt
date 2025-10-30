package com.example.learnlog.di

import android.content.Context
import com.example.learnlog.data.db.AppDatabase
import com.example.learnlog.data.dao.*
import com.example.learnlog.data.repository.NoteRepository
import com.example.learnlog.data.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideSessionLogDao(appDatabase: AppDatabase): SessionLogDao {
        return appDatabase.sessionLogDao()
    }

    @Provides
    fun provideSubjectDao(appDatabase: AppDatabase): SubjectDao {
        return appDatabase.subjectDao()
    }

    @Provides
    fun provideNoteDao(appDatabase: AppDatabase): NoteDao {
        return appDatabase.noteDao()
    }

    @Provides
    @Singleton
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
        return appDatabase.taskDao()
    }

    @Provides
    fun provideDailyRollupDao(appDatabase: AppDatabase): DailyRollupDao {
        return appDatabase.dailyRollupDao()
    }

    @Provides
    @Singleton
    fun provideNoteRepository(noteDao: NoteDao): NoteRepository {
        return NoteRepository(noteDao)
    }

    @Provides
    @Singleton
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository {
        return TaskRepository(taskDao)
    }
}
