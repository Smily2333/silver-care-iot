# 小程序认证与设备权限

## 登录

小程序调用 `wx.login` 获得一次性 `code`，随后请求：

```http
POST /api/miniapp/auth/login
Content-Type: application/json

{"code":"微信登录临时码"}
```

后端使用服务器环境变量中的 AppID 和 AppSecret 调用微信 `jscode2session`，再返回平台自己的随机令牌：

```json
{
  "accessToken": "opaque-random-token",
  "expiresAt": "2026-08-16T00:00:00Z"
}
```

后续 `/api/miniapp/**` 请求均需携带：

```http
Authorization: Bearer opaque-random-token
```

数据库只保存令牌的 SHA-256 哈希，不保存原始令牌，也不保存微信 `session_key`。

## 绑定设备

```http
POST /api/miniapp/devices/bind
Authorization: Bearer ...
Content-Type: application/json

{"deviceNo":"2016001000","ownerName":"张奶奶"}
```

- 未绑定设备：建立当前微信用户与设备的绑定。
- 已由当前用户绑定：允许更新佩戴人姓名。
- 已由其他用户绑定：返回 `409 Conflict`，不得覆盖。
- 查询健康、位置和跌倒数据以及修改姓名前，后端均再次校验绑定关系。

当前采用单一主账号模型。家庭成员共享需要后续增加由主账号发起的邀请和撤销流程。
