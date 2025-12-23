import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ArrayUtil;
import com.cdb.K01KeepApi;
import com.cdb.K01Query;


public class Start {

    public static void main(String[] args) {
        if(ArrayUtil.isEmpty(args)){
            Console.log("启动参数不能为空");
            return;
        }
        switch (args[0]) {
            case "K01Query":
                K01Query.main();
                break;
            case "K01KeepApi":
                K01KeepApi.main();
                break;
            default:
                Console.log("启动参数错误");
        }

        Thread.setDefaultUncaughtExceptionHandler((t,e)->{
            e.printStackTrace();
        });
    }

}
