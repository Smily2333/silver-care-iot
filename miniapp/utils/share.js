const SHARE_TITLE = '银龄守护｜智能手表日常照护'
const SHARE_PATH = '/pages/index/index'
const SHARE_IMAGE_URL = '/images/share-cover.png'

function enableSharing() {
  if (typeof wx.showShareMenu !== 'function') return
  wx.showShareMenu({
    menus: ['shareAppMessage', 'shareTimeline'],
    fail: () => {
      // 旧版微信可能不支持分享到朋友圈，不影响分享给朋友。
    }
  })
}

function getShareAppMessage() {
  return {
    title: SHARE_TITLE,
    path: SHARE_PATH,
    imageUrl: SHARE_IMAGE_URL
  }
}

function getShareTimeline() {
  return {
    title: SHARE_TITLE,
    query: '',
    imageUrl: SHARE_IMAGE_URL
  }
}

module.exports = {
  enableSharing,
  getShareAppMessage,
  getShareTimeline
}
