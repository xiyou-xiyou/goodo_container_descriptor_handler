import classloader.GoodoClassLoader;
import classloader.JarFileReader;
import extern.ExternCarrier;
import org.apache.maven.plugins.assembly.filter.ContainerDescriptorHandler;
import org.apache.maven.plugins.assembly.utils.AssemblyFileUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResource;
import util.ExternUtil;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Component(role = ContainerDescriptorHandler.class, hint = "custom")
public class GoodoConfigHandler implements ContainerDescriptorHandler {

    private String comment;

    private final Map<String, List<String>> catalog = new HashMap<>();

    /**
     * 方法描述符号
     */
    private static final Map<String, String> paramsDescriptor;

    /**
     * 所有的jar包路径
     */
    private static final List<JarFileReader> jarList = new ArrayList<>();

    /**
     * extern
     */
    private final Map<String, List<ExternCarrier>> externCarriers = new HashMap<>();

    private static final List<String> propertiesField;

    private boolean excludeOverride = false;

    private GoodoClassLoader goodoClassLoader;

    static {
        propertiesField = new ArrayList<>();
        propertiesField.add("groupId");
        propertiesField.add("artifactId");
        propertiesField.add("version");
        propertiesField.add("publish.time");
        paramsDescriptor = new HashMap<>();
        paramsDescriptor.put("B", "byte");
        paramsDescriptor.put("C", "char");
        paramsDescriptor.put("S", "shot");
        paramsDescriptor.put("I", "int");
        paramsDescriptor.put("F", "float");
        paramsDescriptor.put("D", "double");
        paramsDescriptor.put("J", "long");
        paramsDescriptor.put("Z", "boolean");

    }

    /**
     * 文件合并
     *
     * @param archiver 当前所需要读取的所有文件信息
     * @throws ArchiverException 文件缓存路径异常
     */
    @Override
    public void finalizeArchiveCreation(Archiver archiver) throws ArchiverException {
        archiver.getResources().forEachRemaining(a -> {
        }); // 提示isSelected()调用
        File destFile = archiver.getDestFile();
        String replace = destFile.getAbsolutePath().replace(".zip", ".dar");
        File newPath = new File(replace);
        // 设置新路径的文件名
        archiver.setDestFile(newPath);

        for (Map.Entry<String, List<String>> entry : catalog.entrySet()) {
            String name = entry.getKey();
            String fname = new File(name).getName();

            Path p;
            try {
                p = Files.createTempFile("Goodo-" + fname, ".tmp");
            } catch (IOException e) {
                throw new ArchiverException("Cannot create temporary file to finalize archive creation", e);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                writer.write("# " + comment);
                for (String line : entry.getValue()) {
                    writer.newLine();
                    writer.write(line);
                }
            } catch (IOException e) {
                throw new ArchiverException("Error adding content of " + fname + " to finalize archive creation", e);
            }

            File file = p.toFile();
            file.deleteOnExit();
            excludeOverride = true;
            archiver.addFile(file, name);
            excludeOverride = false;
        }

        File file = new File(ExternUtil.Extern_DO);

        Path doPath;
        try {
            doPath = Files.createTempFile("assembly-" + file.getName(), ".tmp");
        } catch (IOException e) {
            throw new ArchiverException("Cannot create temporary file to finalize archive creation", e);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(doPath, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, List<ExternCarrier>> entry : externCarriers.entrySet()) {
                // 类全路径
                StringBuilder paramField = new StringBuilder("methods: ");
                String key = entry.getKey();
                // 需要拼接的所有方法描述
                for (ExternCarrier line : entry.getValue()) {
                    paramField.append(key).append(".").append(line.getMethodName()).append("(");
                    List<String> params = line.getParams();
                    paramField.append(ExternUtil.listToString(params));
                    String returnType = line.getReturnType();
                    paramField.append(")").append(returnType).append(",");
                }
                int length = paramField.length();
                String substring = paramField.substring(0, length - 1);
                String writeString = substring + ";";
                writer.newLine();
                writer.write(writeString);
            }

            File pathFile = doPath.toFile();
            pathFile.deleteOnExit();
            excludeOverride = true;
            archiver.addFile(pathFile, ExternUtil.Extern_DO);
            excludeOverride = false;
        } catch (IOException e) {
            throw new ArchiverException("Error adding content of " + file.getName() + " to finalize archive creation", e);
        }
    }

    @Override
    public void finalizeArchiveExtraction(UnArchiver unArchiver) throws ArchiverException {

    }

    @Override
    public List<String> getVirtualFiles() {
        return new ArrayList<>(catalog.keySet());
    }

    @Override
    public boolean isSelected(FileInfo fileInfo) throws IOException {

        if (excludeOverride) {
            return true;
        }
        String name = AssemblyFileUtils.normalizeFileInfo(fileInfo);
        if (fileInfo.isFile() && AssemblyFileUtils.isPropertyFile(name)) {
            Properties properties = new Properties();
            properties.load(fileInfo.getContents());
            propertiesField.forEach(v -> {
                if (!properties.containsKey(v)) {
                    throw new RuntimeException("[" + v + "] not found, please check meta.properties");
                }
            });
            catalog.put(name, readLines(fileInfo));
            return false;
        } else if (fileInfo.isFile() && name.endsWith(".class")) {
            if (goodoClassLoader == null) {
                if (fileInfo instanceof PlexusIoFileResource) {
                    PlexusIoFileResource plexusIoFileResource = (PlexusIoFileResource) fileInfo;
                    File path = plexusIoFileResource.getFile();
                    String pathString = path.getPath();
                    String classPath = fileInfo.getName();
                    goodoClassLoader = new GoodoClassLoader(getClassPath(pathString, classPath), this.getClass().getClassLoader(), jarList);
                } else
                    throw new RuntimeException("File type unable to distinguish :" + fileInfo.getClass().getName());
            }
            String split = name.split("\\.")[0];
            String className = split.replace("/", ".");
            goodoClassLoader.findClass(className);
            doExternProcessor(className);
            return false;
        } else if (fileInfo.isFile() && name.endsWith(".do")) {
            return true;
        } else if (fileInfo.isFile() && name.endsWith(".jar")) {
            jarList.add(new JarFileReader(fileInfo.getContents()));
            return true;
        } else {
            return false;
        }
    }

    private List<String> readLines(FileInfo fileInfo) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileInfo.getContents(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.toList());
        }
    }

    /**
     * 获取当前的类class路径
     *
     * @param path      当前文件全路径
     * @param classpath 当前类相对路径
     * @return 返回当前的class路径
     */
    public String getClassPath(String path, String classpath) {
        int index = path.indexOf(classpath);
        return path.substring(0, index);
    }

    public void doExternProcessor(String className) {
        try {
            Class<?> aClass = Class.forName(className, false, goodoClassLoader);
            String packetClassName = aClass.getName();

            if (externCarriers.containsKey(packetClassName))
                return;

            Method[] methods = aClass.getMethods();
            List<ExternCarrier> externContainer = new ArrayList<>();
            for (Method method : methods) {
                int modifiers = method.getModifiers();

                // 辨别当前的方法是否为静态方法
                if (Modifier.isStatic(modifiers)) {
                    ExternCarrier externCarrier = new ExternCarrier();
                    // 获取参数
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    // 获取返回值
                    Class<?> returnType = method.getReturnType();
                    List<String> params = new ArrayList<>();
                    // 处理参数描述
                    for (Class<?> parameterType : parameterTypes) {
                        String paramName = parameterType.getName();
                        String paramField = typeProcessor(paramName);
                        params.add(paramField);
                    }
                    externCarrier.setParams(params);
                    // 处理返回值描述
                    String returnField = typeProcessor(returnType.getName());
                    externCarrier.setReturnType(returnField);
                    externCarrier.setMethodName(method.getName());
                    // 添加进入容器
                    externContainer.add(externCarrier);
                }
            }
            if (externContainer.size() != 0) {
                externCarriers.put(packetClassName, externContainer);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private String typeProcessor(String paramName) {
        StringBuilder paramField;
        if (ExternUtil.isArray(paramName)) {
            // 获取维度
            int arrayCount = ExternUtil.descriptorArrayCount(paramName);
            // 去除符号特写与维度描述
            String notArrayParam = ExternUtil.descriptorNotArray(paramName);

            // 判断当前是否为基本类型描述符
            if (notArrayParam.length() == 1) {
                paramField = new StringBuilder(paramsDescriptor.get(notArrayParam));
            } else {
                paramField = new StringBuilder(notArrayParam);
            }

            // 添加维度特写
            for (int i = 0; i < arrayCount; i++) {
                paramField.append("[]");
            }
        } else {
            // 去除符号
            paramField = new StringBuilder(ExternUtil.descriptorNotSymbol(paramName));
        }
        return paramField.toString();
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
