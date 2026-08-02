Page({
  enterDemo() {
    wx.navigateTo({ url: '/pages/overview/overview?demo=1' })
  },

  openPrivacy() {
    wx.navigateTo({ url: '/pages/privacy/privacy' })
  },

  backHome() {
    wx.reLaunch({ url: '/pages/index/index' })
  }
})
