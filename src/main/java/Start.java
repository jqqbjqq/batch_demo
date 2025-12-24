import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ArrayUtil;
import com.cdb.K01DateQuery;
import com.cdb.K01DayQuery;
import com.cdb.K01RefreshApi;


public class Start {

    public static void main(String[] args) {
        if(ArrayUtil.isEmpty(args)){
            Console.log("启动参数不能为空");
            return;
        }
        switch (args[0]) {
            case "K01DayQuery":
                K01DayQuery.main();
                break;
            case "K01DateQuery":
                K01DateQuery.main();
                break;
            case "K01RefreshApi":
                K01RefreshApi.main();
                break;
            default:
                Console.log("启动参数错误");
        }

        Thread.setDefaultUncaughtExceptionHandler((t,e)->{
            e.printStackTrace();
        });
    }

}
