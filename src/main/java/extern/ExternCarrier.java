package extern;

import java.util.ArrayList;
import java.util.List;

/**
 * 同位方法的载体
 */
public class ExternCarrier {

    /**
     * 参数
     */
    private List<String> params = new ArrayList<>();

    /**
     * 返回值
     */
    private String returnType;

    private String methodName;

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
