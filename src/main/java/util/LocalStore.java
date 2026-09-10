package util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Store 的文件系统实现(生产用;B 模块维护);
 * 目录与文件名来自 SaveConfig(默认用户目录隐藏子目录)
 */
public class LocalStore implements Store {

    /** 写入(目录/文件名由调用方给定) */
    @Override
    public void saveText(String file, String content) {
        Path path = Path.of(file);
        try {
            // 首次运行自动创建缺失的父目录(默认用户目录隐藏子目录)
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("写入失败: " + file, e);
        }
    }

    /** 读取(文件缺失返回 empty) */
    @Override
    public Optional<String> loadText(String file) {
        Path path = Path.of(file);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("读取失败: " + file, e);
        }
    }

    /** 删除 */
    @Override
    public void delete(String file) {
        try {
            Files.deleteIfExists(Path.of(file));
        } catch (IOException e) {
            throw new UncheckedIOException("删除失败: " + file, e);
        }
    }

    /** 是否存在 */
    @Override
    public boolean exists(String file) {
        return Files.exists(Path.of(file));
    }
}
