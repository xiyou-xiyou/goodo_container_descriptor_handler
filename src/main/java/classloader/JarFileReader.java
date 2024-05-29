package classloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class JarFileReader {

    private final JarInputStream jarInput;

    private final HashMap<String, byte[]> entriesStreamMap;

    public JarFileReader(InputStream in) throws IOException {
        jarInput = new JarInputStream(in);
        entriesStreamMap = new HashMap<>();
        init();
    }

    private void init() throws IOException {
        JarEntry entry = jarInput.getNextJarEntry();
        while (entry != null) {
            if (entry.getName().endsWith(".class")) {
                copyInputStream(jarInput, entry.getName());
            }
            entry = jarInput.getNextJarEntry();
        }
    }

    private void copyInputStream(InputStream in, String entryName) throws IOException {
        if (!entriesStreamMap.containsKey(entryName)) {
            ByteArrayOutputStream _copy = new ByteArrayOutputStream();
            int chunk;
            byte[] data = new byte[256];
            while (-1 != (chunk = in.read(data))) {
                _copy.write(data, 0, chunk);
            }
            entriesStreamMap.put(entryName, _copy.toByteArray());
        }
    }

    public HashMap<String, byte[]> getEntriesStreamMap() {
        return entriesStreamMap;
    }
}
