const { enableSharing, getShareAppMessage, getShareTimeline } = require('../../utils/share')

Page({
  onLoad() {
    enableSharing()
  },

  onShareAppMessage() {
    return getShareAppMessage()
  },

  onShareTimeline() {
    return getShareTimeline()
  },

  copyPrivacyUrl() {
    wx.setClipboardData({
      data: 'https://nkucare.cloud/privacy/',
      success: () => wx.showToast({ title: '网址已复制', icon: 'success' })
    })
  },

  copyContactEmail() {
    wx.setClipboardData({
      data: '2401346847@qq.com',
      success: () => wx.showToast({ title: '邮箱已复制', icon: 'success' })
    })
  },

  openOfficialPrivacy() {
    if (typeof wx.openPrivacyContract !== 'function') {
      wx.showModal({
        title: '查看隐私保护指引',
        content: '当前微信版本暂不支持直接打开。请升级微信后重试，或通过小程序右上角菜单查看隐私保护指引。',
        showCancel: false
      })
      return
    }

    wx.openPrivacyContract({
      fail: () => {
        wx.showModal({
          title: '暂时无法打开',
          content: '请确认运营者已在微信公众平台完成《用户隐私保护指引》配置。',
          showCancel: false
        })
      }
    })
  }
})
