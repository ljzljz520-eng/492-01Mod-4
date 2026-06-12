import request from '@/utils/request'

export const disputeApi = {
  createSupervisorDeduction(data) {
    return request({
      url: '/dispute/supervisor-deduction',
      method: 'post',
      data
    })
  },
  createWorkerAppeal(data) {
    return request({
      url: '/dispute/worker-appeal',
      method: 'post',
      data
    })
  },
  workerSubmit(id, data) {
    return request({
      url: `/dispute/${id}/worker-submit`,
      method: 'post',
      data
    })
  },
  supervisorSubmit(id, data) {
    return request({
      url: `/dispute/${id}/supervisor-submit`,
      method: 'post',
      data
    })
  },
  getById(id) {
    return request({
      url: `/dispute/${id}`,
      method: 'get'
    })
  },
  page(params) {
    return request({
      url: '/dispute/page',
      method: 'get',
      params
    })
  },
  startArbitration(id) {
    return request({
      url: `/dispute/${id}/start-arbitration`,
      method: 'put'
    })
  }
}
