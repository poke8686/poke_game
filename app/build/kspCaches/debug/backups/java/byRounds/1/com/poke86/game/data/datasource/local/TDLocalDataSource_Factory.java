package com.poke86.game.data.datasource.local;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class TDLocalDataSource_Factory implements Factory<TDLocalDataSource> {
  private final Provider<Context> contextProvider;

  public TDLocalDataSource_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public TDLocalDataSource get() {
    return newInstance(contextProvider.get());
  }

  public static TDLocalDataSource_Factory create(Provider<Context> contextProvider) {
    return new TDLocalDataSource_Factory(contextProvider);
  }

  public static TDLocalDataSource newInstance(Context context) {
    return new TDLocalDataSource(context);
  }
}
