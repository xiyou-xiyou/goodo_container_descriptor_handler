package classloader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GoodoClassLoader extends ClassLoader {

    /**
     * 当前的类class路径
     */
    private final String classPath;

    public GoodoClassLoader(String classPath, ClassLoader classLoader) {
        super(classLoader);
        this.classPath = classPath;
    }

    @Override
    public Class<?> findClass(String name) {
        String path = name.replaceAll("\\.", "/");
        try {
            byte[] classBytes = getClassBytes(classPath + path + ".class");
            assert classBytes != null;
            return defineClass(name, classBytes, 0, classBytes.length);
        } catch (LinkageError l) {
            return null;
        } catch (Throwable e) {
            throw new RuntimeException("load class error：" + name);
        }
    }

    private byte[] getClassBytes(String path) {
        try (InputStream fis = Files.newInputStream(Paths.get(path)); ByteArrayOutputStream classBytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = fis.read(buffer)) != -1) {
                classBytes.write(buffer, 0, len);
            }
            return classBytes.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
