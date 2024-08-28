package world.xuewei.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件存储工具
 *
 * @author XUEW
 */
@Component
public class OssClient {

    @Value("${local.storage-path}")
    private String localStoragePath;  // 本地存储路径

    /**
     * 保存文件到本地
     */
    public String upload(MultipartFile file, String path) throws IOException {
        if (file == null || path == null) {
            return null;
        }

        return saveToLocal(file, path);
    }

    private String saveToLocal(MultipartFile file, String path) throws IOException {
        String extension = getFileExtension(file);
        String fileName = path + "/" + System.currentTimeMillis() + extension;
        Path destinationFile = Paths.get(localStoragePath, fileName);
        File parentDir = destinationFile.toFile().getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        file.transferTo(destinationFile);
        return destinationFile.toString();
    }

    /**
     * 获取文件的扩展名
     */
    public static String getFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        assert filename != null;
        return filename.substring(filename.lastIndexOf("."));
    }
}
