# 设备通信验证记录

**状态：C6T 固件已完成核心快捷操作真机验证。其他型号或固件仍需重新验证。**

## 已确认并可启用

| 动作 | 命令 | 真机结果 | 启用值 |
|---|---|---|---|
| 立即定位 | `CR` | 约 9 秒后上报 `UD_LTE`；室内 GPS 无效时仍会返回状态 | `LOCATE_NOW` |
| 测量心率 | `hrtstart,1` | 约 34 秒后上报血压/心率，随后上报 `oxygen`；未佩戴时均为 0 | `MEASURE_HEART_RATE` |
| 测量体温 | `bodytemp2` | 约 22 秒后上报 `btemp2`；未佩戴也可能返回正常区间温度 | `MEASURE_TEMPERATURE` |

确认后将对应枚举加入：

```text
SILVER_CARE_PROD_CONFIRMED_DEVICE_ACTIONS=LOCATE_NOW,MEASURE_HEART_RATE,MEASURE_TEMPERATURE
SILVER_CARE_PROD_ALLOW_HEALTH_WITHOUT_WEAR_STATUS=true
```

当前固件在佩戴和取下时均上报 `terminalStatus=00000000`，即使启用 `REMOVE,1` 也没有变化。
因此允许管理员主动测量，但不能把体温结果视为已佩戴证据；心率、血压和血氧全 0 时按无效处理。

## 暂未实现/启用

- `terminalStatus` 的可靠佩戴状态来源；
- `oxygen` 独立入库和有效性展示；
- 组合“立即健康检测”的串行间隔和完成规则；
- 主动血压测量命令；
- `UPLOAD` 定时位置策略及静止行为；
- `AL` 精确分类、跌倒去重和通知渠道；
- 心率、体温定时调度策略。

## 请回填的样本

每组样本记录发送时间、完整下行报文、完整 ACK、后续上报、设备当时是否佩戴，以及各报文时间差。
隐去手机号、姓名等无关信息，但保留设备协议字段顺序和状态位原值。
