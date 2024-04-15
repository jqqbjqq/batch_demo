import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ArrayUtil;
import com.cctv.security.IpWarnQuery;
import com.cctv.security.VideoWarn;
import com.cctv.security.WarnFileMerge;

/**
 * @author jiqq
 * @date 2024/4/11
 * @description
 */
public class Start {

    public static void main(String[] args) {
        if(ArrayUtil.isEmpty(args)){
            Console.log("启动参数不能为空");
            return;
        }
        switch (args[0]) {
            case "WarnFileMerge":
                WarnFileMerge.main(args[1]);
                break;
            case "VideoWarn":
                VideoWarn.main(args[1]);
                break;
            case "IpWarnQuery":
                IpWarnQuery.main(args[1],args[2],args[3]);
                break;
            default:
                Console.log("启动参数错误");
        }
    }
}
