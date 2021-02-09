package me.XXX.eesearcher.module;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.throwingproviders.CheckedProvider;
import me.XXX.eesearcher.SystemConfiguration;

import java.io.File;
import java.io.IOException;

public class SystemConfigurationProvider implements CheckedProvider<SystemConfiguration> {

    @Inject
    @Named("internal-config")
    private File internalFile;

    @Override
    public SystemConfiguration get() throws IOException {
        boolean exists = internalFile.exists();
        if (!exists) {
            internalFile.createNewFile();
        }
        SystemConfiguration configuration = new SystemConfiguration(this.internalFile);
        if (!exists) {
            configuration.save();
        }
        return configuration;
    }

}
