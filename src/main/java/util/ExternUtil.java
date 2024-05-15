package util;

import java.util.List;

public class ExternUtil {

    public static String Extern_DO = "META-INF/extern.do";

    /**
     * 获取当前的参数维度
     *
     * @param descriptor 描述符
     * @return 返回当前维度
     */
    public static int descriptorArrayCount(String descriptor) {
        String replace = descriptor.replace(";", "");
        int length = replace.length();
        int notArrayLength = replace.replace("[", "").length();
        return length - notArrayLength;
    }

    /**
     * 去除维度的参数描述符
     *
     * @param descriptor 描述符
     * @return 当前去除维度的描述符
     */
    public static String descriptorNotArray(String descriptor) {
        String replace = descriptor.replace(";", "");
        int length = replace.length();
        return replace.replace("[", "");
    }

    /**
     * 去除符号位的描述符
     *
     * @param descriptor 描述符
     * @return 当前去除符号的描述符
     */
    public static String descriptorNotSymbol(String descriptor) {
        return descriptor.replace(";", "");
    }

    /**
     * 判断当前描述符是否存在维度特写
     *
     * @param descriptor 描述符
     * @return 返回判断结果
     */
    public static boolean isArray(String descriptor) {
        return descriptor.contains("[");
    }

    /**
     * 集合转换字符串，以逗号分割
     *
     * @param list 需要拼接的集合
     * @return 返回转换的字符串
     */
    public static String listToString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                if (i < list.size() - 1) {
                    sb.append(list.get(i)).append(",");
                } else {
                    sb.append(list.get(i));
                }
            }
        }
        return sb.toString();
    }
}
