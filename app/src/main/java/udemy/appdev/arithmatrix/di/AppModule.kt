package udemy.appdev.arithmatrix.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import udemy.appdev.arithmatrix.data.local.HistoryDAO
import udemy.appdev.arithmatrix.data.local.HistoryDatabase
import udemy.appdev.arithmatrix.data.repository.CurrencyRepository
import udemy.appdev.arithmatrix.data.repository.HistoryRepository
import udemy.appdev.arithmatrix.engine.CalculatorEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCalculatorEngine(): CalculatorEngine = CalculatorEngine()

    @Provides
    @Singleton
    fun provideDatabase(app: Application): HistoryDatabase {
        return Room.databaseBuilder(
            app,
            HistoryDatabase::class.java,
            "history_db"
        )
            .addMigrations(HistoryDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideHistoryDao(db: HistoryDatabase): HistoryDAO = db.historyDao()

    @Provides
    @Singleton
    fun provideHistoryRepository(dao: HistoryDAO): HistoryRepository = HistoryRepository(dao)

    @Provides
    @Singleton
    fun provideCurrencyRepository(@ApplicationContext context: Context): CurrencyRepository {
        return CurrencyRepository(context)
    }
}