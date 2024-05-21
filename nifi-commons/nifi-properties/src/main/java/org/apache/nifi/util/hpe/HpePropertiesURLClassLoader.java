package org.apache.nifi.util.hpe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * HPE Hadoop properties URL Class Loader uses the Current Thread Class Loader as the parent and loads libraries from a standard directory
 */
public class HpePropertiesURLClassLoader extends URLClassLoader {
    private static final String STANDARD_DIRECTORY = "lib/hpe-properties";
    private static final ClassLoader CONTEXT_CLASS_LOADER = Thread.currentThread().getContextClassLoader();
    private static final Logger logger = LoggerFactory.getLogger(HpePropertiesURLClassLoader.class);

    public HpePropertiesURLClassLoader() {
        this(CONTEXT_CLASS_LOADER);
    }

    public HpePropertiesURLClassLoader(final ClassLoader parentClassLoader) {
        super(getPropertyProtectionUrls(), parentClassLoader);
    }

    private static URL[] getPropertyProtectionUrls() {
        final Path standardDirectory = Paths.get(STANDARD_DIRECTORY);
        if (Files.exists(standardDirectory)) {
            try (final Stream<Path> files = Files.list(standardDirectory)) {
                return files.map(Path::toUri)
                        .map(uri -> {
                            try {
                                return uri.toURL();
                            } catch (final MalformedURLException e) {
                                throw new UncheckedIOException(String.format("Processing HPE properties libraries failed [%s]", standardDirectory), e);
                            }
                        })
                        .toArray(URL[]::new);
            } catch (final IOException e) {
                throw new UncheckedIOException(String.format("Loading HPE properties libraries failed [%s]", standardDirectory), e);
            }
        } else {
            logger.error("HPE configuration libraries directory [{}] not found", standardDirectory);
            return new URL[0];
        }
    }
}
