import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    Authorization: 'Basic ' + btoa(
      `${import.meta.env.VITE_ADMIN_USERNAME}:${import.meta.env.VITE_ADMIN_PASSWORD}`
    )
  }
})

export default instance
