package com.gmail.takenokoii78.blockpropertyaccessor;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@NullMarked
public class ZipCompressor {
    private final Plugin plugin;

    private final Path directory;

    public ZipCompressor(Plugin plugin, Path directory) {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException();
        }

        this.plugin = plugin;
        this.directory = directory;
    }

    private ComponentLogger getComponentLogger() {
        return plugin.getComponentLogger();
    }

    public void compress(Path out) {
        if (Files.exists(directory)) {
            getComponentLogger().info("対象ディレクトリを検出しました: {}", directory);

            try (final ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(out.toFile())))) {
                copyToZip(zip);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void copyToZip(ZipOutputStream zip) {
        int lastProgress = 0;

        try (final Stream<Path> stream = Files.walk(directory)) {
            final List<Path> paths = stream.toList();

            for (int i = 0; i < paths.size(); i++) {
                final Path path = paths.get(i);

                if (Files.isDirectory(path)) continue;

                final String name = directory.relativize(path).toString().replace(File.separatorChar, '/');
                zip.putNextEntry(new ZipEntry(name));
                Files.copy(path, zip);
                zip.closeEntry();

                final int progress = (i + 1) * 100 / paths.size();
                if (progress % 10 == 0 && lastProgress != progress) {
                    lastProgress = progress;
                    getComponentLogger().info("進捗率: {} %", progress);
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
