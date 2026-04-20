package com.poke86.game.ui.games.towerdefense;

import com.poke86.game.domain.repository.TDSaveRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class TowerDefenseViewModel_Factory implements Factory<TowerDefenseViewModel> {
  private final Provider<TDSaveRepository> saveRepositoryProvider;

  public TowerDefenseViewModel_Factory(Provider<TDSaveRepository> saveRepositoryProvider) {
    this.saveRepositoryProvider = saveRepositoryProvider;
  }

  @Override
  public TowerDefenseViewModel get() {
    return newInstance(saveRepositoryProvider.get());
  }

  public static TowerDefenseViewModel_Factory create(
      Provider<TDSaveRepository> saveRepositoryProvider) {
    return new TowerDefenseViewModel_Factory(saveRepositoryProvider);
  }

  public static TowerDefenseViewModel newInstance(TDSaveRepository saveRepository) {
    return new TowerDefenseViewModel(saveRepository);
  }
}
