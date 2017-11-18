package micdm.yeelight.di

import android.content.Context
import android.view.LayoutInflater
import dagger.Module
import dagger.Provides
import micdm.yeelight.App

@Module
class AppModule(private val app: App) {

    @Provides
    @AppScope
    fun provideContext(): Context = app.applicationContext

    @Provides
    @AppScope
    fun provideLayoutInflater(context: Context): LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
}
