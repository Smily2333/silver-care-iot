# 设备通信待确认清单

**状态：需要人工真机验证。未确认能力在生产配置中默认关闭。**

## 已实现但默认关闭

| 动作 | 候选命令 | 需要提供的真机证据 | 启用值 |
|---|---|---|---|
| 立即定位 | `CR` | ACK 原始报文、随后位置命令、典型耗时 | `LOCATE_NOW` |
| 测量心率 | `hrtstart,1` | ACK、`bphrt` 样本、是否同时产生有效血压 | `MEASURE_HEART_RATE` |
| 测量体温 | `bodytemp2` | ACK、`btemp2` 样本、失败值和耗时 | `MEASURE_TEMPERATURE` |

确认后将对应枚举加入：

```text
SILVER_CARE_PROD_CONFIRMED_DEVICE_ACTIONS=LOCATE_NOW
```

多个值使用逗号分隔。心率和体温还需要确认佩戴状态位；确认前
`SILVER_CARE_PROD_ALLOW_HEALTH_WITHOUT_WEAR_STATUS` 必须保持 `false`。

## 暂未实现/启用

- `terminalStatus` 的佩戴、取下、静止、跌倒 Bit 掩码；
- 组合“立即健康检测”的串行间隔和完成规则；
- 主动血压测量命令；
- `UPLOAD` 定时位置策略及静止行为；
- `AL` 精确分类、跌倒去重和通知渠道；
- 心率、体温定时调度策略。

## 请回填的样本

每组样本记录发送时间、完整下行报文、完整 ACK、后续上报、设备当时是否佩戴，以及各报文时间差。
隐去手机号、姓名等无关信息，但保留设备协议字段顺序和状态位原值。
