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
import java.nio.file.Paths;
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
                directory(directory.getNameCount(), directory, zip);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void directory(int rootDirectoryNameCount, Path path, ZipOutputStream zip) throws IOException {
        try (final Stream<Path> stream = Files.list(path)) {
            stream.forEach(p -> {
                try {
                    final String name = p.subpath(rootDirectoryNameCount, p.getNameCount())
                        .toString()
                        .replace(File.separatorChar, '/');

                    if (Files.isDirectory(p)) {
                        zip.putNextEntry(new ZipEntry(name + '/'));
                        zip.closeEntry();
                        getComponentLogger().info("ディレクトリ {} を .zip 内に配置しました; コピー元の内部を探索します", name);
                        directory(rootDirectoryNameCount, p, zip);
                    }
                    else {
                        zip.putNextEntry(new ZipEntry(name));
                        Files.copy(p, zip);
                        zip.closeEntry();
                        getComponentLogger().info("ファイル {} を .zip 内に配置しました", name);
                    }
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
