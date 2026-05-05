import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 12000,
  withCredentials: true
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'success' in body) {
      if (!body.success) {
        const message = body.message || '请求失败'
        if (message === '请先登录' || message === '未登录') {
          return Promise.reject(new Error('请登录后使用'))
        }
        return Promise.reject(new Error(message))
      }
      return body.data
    }
    return body
  },
  (error) => {
    const rawMessage = error.response?.data?.message || error.message || '网络异常'
    const message = rawMessage === '请先登录' || rawMessage === '未登录' ? '请登录后使用' : rawMessage
    return Promise.reject(new Error(message))
  }
)

export default request
