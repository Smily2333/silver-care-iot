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

- 新关系：建立当前微信账号与设备的绑定。
- 每块手表最多绑定 4 个微信账号，每个微信账号最多绑定 4 块手表。
- 同一微信账号与同一手表只保存一条绑定关系，重复绑定视为更新佩戴人称呼。
- 任一侧达到 4 个绑定后，再建立新关系返回 `409 Conflict`。
- 查询健康、位置和跌倒数据以及修改姓名前，后端均再次校验绑定关系。

当前采用对称的家庭共享模型；任一已绑定账号均可查看设备数据和修改该设备的佩戴人称呼。
