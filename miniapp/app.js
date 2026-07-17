const { loginMiniapp } = require('./utils/api')

App({
  onLaunch() {
    loginMiniapp().catch(() => {
      // 页面请求时会再次登录并展示具体错误。
    })
  },
  globalData: {}
})
