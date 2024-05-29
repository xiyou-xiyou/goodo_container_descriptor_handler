package classloader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoodoClassLoader extends ClassLoader {

    /**
     * 当前的类class路径
     */
    private final String classPath;

    /**
     * 所有jar包内的class
     */
    private final Map<String, byte[]> classBytesMap = new HashMap<>();

    public GoodoClassLoader(String classPath, ClassLoader classLoader, List<JarFileReader> jarFileReaderList) {
        super(classLoader);
        this.classPath = classPath;
        init(jarFileReaderList);
    }

    private void init(List<JarFileReader> jarFileReaderList) {
        for (JarFileReader jarFile : jarFileReaderList) {
            loadJar(jarFile);
        }
    }

    @Override
    public Class<?> findClass(String name) {
        String path = name.replaceAll("\\.", "/") + ".class";
        try {
            byte[] classBytes = getClassBytes(classPath + path);
            if (classBytes == null) {
                classBytes = classBytesMap.get(path);
            }
            return defineClass(name, classBytes, 0, classBytes.length);
        } catch (LinkageError l) {
            return null;
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    private byte[] getClassBytes(String path) {
        try (InputStream fis = Files.newInputStream(Paths.get(path)); ByteArrayOutputStream classBytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                classBytes.write(buffer, 0, len);
            }
            return classBytes.toByteArray();
        } catch (NoSuchFileException n) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void loadJar(JarFileReader jarFile) {
        HashMap<String, byte[]> entriesStreamMap = jarFile.getEntriesStreamMap();
        classBytesMap.putAll(entriesStreamMap);
    }
}
