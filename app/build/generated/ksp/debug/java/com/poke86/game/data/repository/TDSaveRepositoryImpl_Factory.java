package com.poke86.game.data.repository;

import com.poke86.game.data.datasource.local.TDLocalDataSource;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class TDSaveRepositoryImpl_Factory implements Factory<TDSaveRepositoryImpl> {
  private final Provider<TDLocalDataSource> localProvider;

  public TDSaveRepositoryImpl_Factory(Provider<TDLocalDataSource> localProvider) {
    this.localProvider = localProvider;
  }

  @Override
  public TDSaveRepositoryImpl get() {
    return newInstance(localProvider.get());
  }

  public static TDSaveRepositoryImpl_Factory create(Provider<TDLocalDataSource> localProvider) {
    return new TDSaveRepositoryImpl_Factory(localProvider);
  }

  public static TDSaveRepositoryImpl newInstance(TDLocalDataSource local) {
    return new TDSaveRepositoryImpl(local);
  }
}
