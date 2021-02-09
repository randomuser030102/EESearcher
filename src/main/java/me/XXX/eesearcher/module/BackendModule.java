package me.XXX.eesearcher.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.throwingproviders.CheckedProvides;
import com.google.inject.throwingproviders.ThrowingProviderBinder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import me.XXX.eesearcher.SearchHistoryController;
import me.XXX.eesearcher.SystemConfiguration;
import me.XXX.eesearcher.common.IndexDataController;
import me.XXX.eesearcher.common.Parser;
import me.XXX.eesearcher.data.DataUtil;
import me.XXX.eesearcher.data.SubjectDatabase;
import me.XXX.eesearcher.data.Subjects;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BackendModule extends AbstractModule {


    @Override
    protected void configure() {
        ThrowingProviderBinder.forModule(this);
        bind(DataUtil.class).asEagerSingleton();
        bind(Parser.class).asEagerSingleton();
        bind(SubjectDatabase.class).asEagerSingleton();
        bind(Subjects.class).asEagerSingleton();
        bind(IndexDataController.class).asEagerSingleton();
        bind(SearchHistoryController.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    @Named("internal-config")
    public @NotNull File provideConfigFile() {
        final File file = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile();
        return new File(file, "backend.properties");
    }

    @Provides
    @Singleton
    @CheckedProvides(SystemConfigurationProvider.class)
    @Named("internal-config")
    public @NotNull SystemConfiguration provideSystemConfiguration(@NotNull final SystemConfigurationProvider provider) throws IOException {
        return provider.get();
    }

    @Provides
    @Singleton
    @Named("internal-config")
    public @NotNull HikariConfig provideHikariConfig(@NotNull final SystemConfiguration systemConfiguration) {
        final HikariConfig config = new HikariConfig();
        config.setUsername(systemConfiguration.databaseUsername());
        config.setPassword(systemConfiguration.databasePassword());
        config.setJdbcUrl("jdbc:h2://" + systemConfiguration.databasePath().getAbsolutePath());
        config.setPoolName("backend-pool");
        config.setMaximumPoolSize(systemConfiguration.maxIOThreads() == -1 ? Runtime.getRuntime().availableProcessors() : systemConfiguration.maxIOThreads());
        return config;
    }

    @Provides
    @Singleton
    @Named("internal-pool")
    public @NotNull HikariPool provideInternalPool(@Named("internal-config") HikariConfig config) {
        return new HikariPool(config);
    }

    @Provides
    @Singleton
    public ScheduledExecutorService provideExecutorService(@NotNull final SystemConfiguration configuration) {
        return Executors.newScheduledThreadPool(configuration.maxIOThreads() == -1 ? Runtime.getRuntime().availableProcessors() : configuration.maxIOThreads());
    }
}
