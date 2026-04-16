# 后端结构

<!-- MANUAL: 本文件已精简，目录树和接口签名由源码 + graphify 图谱提供。请勿自动补全。 -->

> 更新时间: 2026-04-13
> 目录树、接口签名请用 graphify MCP 查询或直接读源码

## expect/actual 服务一览

所有 SDK 服务均为 expect/actual 模式：Android 封装原生 SDK，iOS 委托 Bridge，Desktop 用 Mock。

| 服务 | Android | iOS | Desktop |
|------|---------|-----|---------|
| AuthService | Auth SDK (login + token) | LoginBridge → Swift | Mock |
| UserService | Mock (待接入) | UserBridge → Swift | Mock |
| DeviceTokenService | Risk SDK (指纹+签名) | DeviceTokenBridge → Swift | Mock |
| ChatRoomService | IM SDK | ImBridge → Swift | Mock (回声) |
| ImService | IM SDK | ImBridge → Swift | Mock |
| RtcService | RTC SDK | RtcBridge → Swift | Mock |
| PaymentService | Pay SDK (Google Pay / Custom Pay) | PaymentBridge → Swift (Apple IAP) | Mock |
| AttributionService | Adjust SDK 5.5.0 | AttributionBridge → Swift (Adjust pod) | Mock |

## HttpClient 配置要点

- ContentNegotiation (JSON, ignoreUnknownKeys)
- HttpTimeout: request=30s, connect=10s
- HttpRequestRetry: 3 次，指数退避，验证模式下禁用重试
- Auth Bearer: 自动 token 刷新
- ContentEncoding: gzip
- 拦截器链: Tape 回放 → 签名 → 执行

## 网络安全管线

```
Request 构建
  ├─ defaultRequest: HeaderProvider.getHeaders()
  │    → API_KEY, DEVICE_ID, CODE_TAG, TIMESTAMP, LANGUAGE, TIMEZONE
  │    → MEMBER_ID, NONCESTR (动态), UMID (设备指纹)
  ├─ Auth Bearer: AuthService.getAccessToken() (401 自动刷新)
  └─ HttpSend 拦截: DeviceTokenService.sign(fields) → SIGN_TOKEN
```

## Mock 模式

通过 `AppRuntimeState.isInPreview` 控制，Preview 用 Mock 实现，Production 用真实 API。

## 平台引擎

| 平台 | 引擎 |
|------|------|
| Android / Desktop | OkHttp |
| iOS | Darwin |
