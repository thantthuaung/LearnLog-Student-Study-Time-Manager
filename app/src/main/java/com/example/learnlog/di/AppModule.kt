package com.example.learnlog.di

import android.content.Context
import com.example.learnlog.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.example.learnlog.data.repository.*
import com.example.learnlog.data.dao.*
import com.example.learnlog.data.preferences.UserPreferences
import com.example.learnlog.network.QuoteApiService
import com.example.learnlog.util.DateTimeProvider
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- Networking: Moshi ---
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    // --- Networking: OkHttpClient ---
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Add logging interceptor in debug builds only
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    // --- Networking: Retrofit ---
    @Provides
    @Singleton
    fun provideRetrofit(moshi: Moshi, okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.quotable.io/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // --- Networking: API Services ---
    @Provides
    @Singleton
    fun provideQuoteApiService(retrofit: Retrofit): QuoteApiService {
        return retrofit.create(QuoteApiService::class.java)
    }

    // --- Utilities ---
    @Provides
    @Singleton
    fun provideDateTimeProvider(): DateTimeProvider {
        return DateTimeProvider()
    }

    // --- Repositories (non-database) ---
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
    fun provideTasksRepository(
        taskDao: TaskDao,
        dateTimeProvider: DateTimeProvider
    ): TasksRepository {
        return TasksRepository(taskDao, dateTimeProvider)
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
    fun provideQuoteRepository(
        @ApplicationContext context: Context,
        quoteApiService: QuoteApiService
    ): QuoteRepository {
        return QuoteRepository(context, quoteApiService)
    }

    @Provides
    @Singleton
    fun provideSampleDataPopulator(
        @ApplicationContext context: Context,
        taskDao: TaskDao,
        sessionLogDao: SessionLogDao
    ): com.example.learnlog.data.SampleDataPopulator {
        return com.example.learnlog.data.SampleDataPopulator(
            taskDao,
            sessionLogDao,
            context
        )
    }

    @Provides
    @Singleton
    fun provideDataResetRepository(
        appDatabase: com.example.learnlog.data.db.AppDatabase,
        taskDao: TaskDao,
        sessionLogDao: SessionLogDao,
        noteDao: NoteDao,
        subjectDao: SubjectDao,
        dailyRollupDao: DailyRollupDao
    ): DataResetRepository {
        return DataResetRepository(
            appDatabase,
            taskDao,
            sessionLogDao,
            noteDao,
            subjectDao,
            dailyRollupDao
        )
    }
}
