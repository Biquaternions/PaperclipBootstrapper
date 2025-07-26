package net.serlith.bootstrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class Bootstrapper {

    public static void main(String[] args) {

        Path executablePath;
        Set<String> flags = new LinkedHashSet<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
        {
            final Path bootstrapDir = Paths.get(".boot");
            try {
                Files.createDirectories(bootstrapDir);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create .boot directory", e);
            }

            // Load native library
            final Path libraryPath = bootstrapDir.resolve("bootstrapper.so");
            try {
                Files.createDirectories(bootstrapDir);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create .boot/native directory", e);
            }
            Bootstrapper.cacheBundledFile("native/bootstrapper.so", libraryPath);
            System.load(libraryPath.toAbsolutePath().toString());

            // Cache bundled jar
            final InputStream executableStream = Bootstrapper.class.getResourceAsStream("/META-INF/executable");
            if (executableStream == null) {
                throw new RuntimeException("Unable to locate executable path");
            }
            AtomicReference<String> executableName = new AtomicReference<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(executableStream))) {
                reader.lines().findFirst().ifPresentOrElse(executableName::set, RuntimeException::new);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            executablePath = bootstrapDir.resolve(executableName.get());
            Bootstrapper.cacheBundledFile(String.format("/META-INF/%s", executableName.get()), executablePath);

            // Add extra flags
            final InputStream flagsStream = Bootstrapper.class.getResourceAsStream("/META-INF/flags");
            if (flagsStream == null) {
                throw new RuntimeException("Unable to locate executable path");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(flagsStream))) {
                reader.lines().filter(Bootstrapper::isNotBlank).forEach(flags::add);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        Bootstrapper.launchPaperclip(flags.toArray(new String[0]), executablePath.toString());
    }

    private static void cacheBundledFile(String resource, Path destination) {
        try (final InputStream resourceStream = resource.startsWith("/META-INF") ? Bootstrapper.class.getResourceAsStream(resource) : Bootstrapper.class.getClassLoader().getResourceAsStream(resource)) {
            if (resourceStream == null) {
                throw new RuntimeException("Unable to locate " + resource);
            }
            Files.copy(resourceStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isNotBlank(String string) {
        return !string.isBlank();
    }

    private static native void launchPaperclip(String[] flags, String jarPath);

}