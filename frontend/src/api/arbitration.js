import request from '@/utils/request'

export const arbitrationApi = {
  doArbitration(data) {
    return request({
      url: '/arbitration',
      method: 'post',
      data
    })
  },
  getById(id) {
    return request({
      url: `/arbitration/${id}`,
      method: 'get'
    })
  },
  getByDisputeId(disputeId) {
    return request({
      url: `/arbitration/dispute/${disputeId}`,
      method: 'get'
    })
  },
  page(params) {
    return request({
      url: '/arbitration/page',
      method: 'get',
      params
    })
  }
}
