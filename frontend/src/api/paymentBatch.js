import request from '@/utils/request'

export const paymentBatchApi = {
  create(data) {
    return request({
      url: '/payment-batch',
      method: 'post',
      data
    })
  },
  getById(id) {
    return request({
      url: `/payment-batch/${id}`,
      method: 'get'
    })
  },
  page(params) {
    return request({
      url: '/payment-batch/page',
      method: 'get',
      params
    })
  },
  markPaid(id, data) {
    return request({
      url: `/payment-batch/${id}/mark-paid`,
      method: 'put',
      data
    })
  },
  getAvailableSettlements(params) {
    return request({
      url: '/payment-batch/available-settlements',
      method: 'get',
      params
    })
  }
}
