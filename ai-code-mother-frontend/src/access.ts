import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import router from '@/router'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

/**
 * 全局权限校验
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser
  // 确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    // 添加 await，确保 App.vue 从后端读取到用户的身份信息之后再进行校验
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }
  const toUrl = to.fullPath
  // 当用户访问路径地址以/admin开头的路径时
  if (toUrl.startsWith('/admin')) {
    // 如果用户没有登录或者用户身份不是admin
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error('没有权限')
      // 重定向到登录页面，让用户去登录
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  next()
})