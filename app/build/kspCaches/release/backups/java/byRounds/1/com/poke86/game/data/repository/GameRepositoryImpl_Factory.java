package com.poke86.game.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class GameRepositoryImpl_Factory implements Factory<GameRepositoryImpl> {
  @Override
  public GameRepositoryImpl get() {
    return newInstance();
  }

  public static GameRepositoryImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GameRepositoryImpl newInstance() {
    return new GameRepositoryImpl();
  }

  private static final class InstanceHolder {
    private static final GameRepositoryImpl_Factory INSTANCE = new GameRepositoryImpl_Factory();
  }
}
