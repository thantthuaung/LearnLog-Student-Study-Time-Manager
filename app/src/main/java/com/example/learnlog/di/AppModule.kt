package com.example.learnlog.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.learnlog.data.repository.InsightsRepository
import com.example.learnlog.data.repository.SubjectRepository
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.dao.SubjectDao
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
    fun provideInsightsRepository(sessionLogDao: SessionLogDao): InsightsRepository {
        return InsightsRepository(sessionLogDao)
    }
}
