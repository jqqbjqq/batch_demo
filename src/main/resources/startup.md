cmd /k ".\jre\bin\java -jar .\jre\warn-handler.jar WarnFileMerge 安全告警.xlsx"

cmd /k ".\jre\bin\java -jar .\jre\warn-handler.jar VideoWarn 视频中台.xlsx,视频中台2.xlsx"

cmd /k ".\jre\bin\java -jar .\jre\warn-handler.jar IpWarnQuery IP查询.xlsx https://10.238.225.33 admin:439746d307256958d8d204160a686e8b"

cmd /k ".\jre\bin\java -jar .\jre\warn-handler.jar AlarmRuleQuery 告警策略.xlsx https://10.232.1.8 admin:334b54d1ca879b077fdfbab0af1d20fe"
