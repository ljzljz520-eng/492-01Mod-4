import request from '@/utils/request'

export const settlementApi = {
  generate(data) {
    return request({
      url: '/settlement',
      method: 'post',
      data
    })
  },
  getById(id) {
    return request({
      url: `/settlement/${id}`,
      method: 'get'
    })
  },
  page(params) {
    return request({
      url: '/settlement/page',
      method: 'get',
      params
    })
  },
  pageWithDetail(params) {
    return request({
      url: '/settlement/page/detail',
      method: 'get',
      params
    })
  },
  confirm(id) {
    return request({
      url: `/settlement/${id}/confirm`,
      method: 'put'
    })
  },
  updateAmount(id, data) {
    return request({
      url: `/settlement/${id}/amount`,
      method: 'put',
      data
    })
  }
}
