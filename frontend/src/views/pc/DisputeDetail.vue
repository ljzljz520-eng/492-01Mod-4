<template>
  <div class="dispute-detail-page">
    <el-card class="mb-4 shadow-sm rounded-lg border-0" :body-style="{ padding: '24px' }">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <el-button @click="goBack" class="rounded-lg">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <span class="text-xl font-bold text-gray-800">工资争议处理</span>
            <el-tag v-if="disputeInfo" :type="getStatusType(disputeInfo.status)" size="large" class="rounded-full">
              {{ getStatusText(disputeInfo.status) }}
            </el-tag>
          </div>
          <div class="flex gap-2">
            <el-button
              v-if="disputeInfo && (disputeInfo.status === 'worker_submitted' || disputeInfo.status === 'supervisor_submitted')"
              type="warning"
              @click="handleStartArbitration"
              class="rounded-lg"
            >
              启动仲裁
            </el-button>
            <el-button
              v-if="disputeInfo && disputeInfo.status === 'arbitrating'"
              type="primary"
              @click="goToArbitration"
              class="rounded-lg"
            >
              进入仲裁
            </el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="dispute-content">
        <div v-if="disputeInfo">
          <h3 class="text-lg font-bold mb-4">基本信息</h3>
          <el-descriptions :column="2" border class="mb-6">
            <el-descriptions-item label="争议单号">{{ disputeInfo.disputeNo }}</el-descriptions-item>
            <el-descriptions-item label="争议类型">
              <el-tag :type="disputeInfo.disputeType === 'supervisor_deduction' ? 'warning' : 'danger'">
                {{ disputeInfo.disputeType === 'supervisor_deduction' ? '主管扣时' : '工人申诉' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="关联结算单ID">{{ disputeInfo.settlementId }}</el-descriptions-item>
            <el-descriptions-item label="工人ID">{{ disputeInfo.workerId }}</el-descriptions-item>
            <el-descriptions-item label="原始工时">{{ disputeInfo.originalHours }} 小时</el-descriptions-item>
            <el-descriptions-item label="主张工时">{{ disputeInfo.claimedHours }} 小时</el-descriptions-item>
            <el-descriptions-item label="原始金额">
              <span class="text-green-600 font-bold">¥{{ disputeInfo.originalAmount }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="主张金额">
              <span class="text-orange-600 font-bold">¥{{ disputeInfo.claimedAmount }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="争议原因" :span="2">
              <div class="text-red-600">{{ disputeInfo.disputeReason }}</div>
            </el-descriptions-item>
            <el-descriptions-item v-if="disputeInfo.finalHours !== null" label="最终工时">
              {{ disputeInfo.finalHours }} 小时
            </el-descriptions-item>
            <el-descriptions-item v-if="disputeInfo.finalAmount !== null" label="最终金额">
              <span class="text-blue-600 font-bold">¥{{ disputeInfo.finalAmount }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <el-row :gutter="20">
            <el-col :span="12">
              <div class="party-section p-4 bg-orange-50 rounded-lg border border-orange-200">
                <div class="flex items-center justify-between mb-4">
                  <h4 class="font-bold text-lg text-orange-800">
                    <el-icon class="mr-1"><User /></el-icon>
                    工人提交
                  </h4>
                  <el-tag v-if="disputeInfo.workerSubmitTime" type="success" size="small">已提交</el-tag>
                  <el-tag v-else type="info" size="small">待提交</el-tag>
                </div>
                <el-descriptions :column="1" size="small" border class="mb-4">
                  <el-descriptions-item label="提交时间">
                    {{ disputeInfo.workerSubmitTime || '未提交' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="工人说明">
                    <div style="white-space: pre-wrap">{{ disputeInfo.workerRemark || '暂无说明' }}</div>
                  </el-descriptions-item>
                </el-descriptions>
                <div class="evidence-list">
                  <div class="text-sm font-medium mb-2 text-gray-700">工人提供的证据：</div>
                  <el-empty v-if="workerEvidences.length === 0" description="暂无证据" :image-size="60" />
                  <div v-else class="grid grid-cols-3 gap-3">
                    <div v-for="(ev, idx) in workerEvidences" :key="idx" class="evidence-item">
                      <el-image
                        v-if="ev.fileUrl"
                        :src="ev.fileUrl"
                        :preview-src-list="workerEvidences.filter(e => e.fileUrl).map(e => e.fileUrl)"
                        :initial-index="workerEvidences.findIndex(e => e.id === ev.id)"
                        fit="cover"
                        class="w-full h-32 rounded-lg border"
                      />
                      <div v-else class="w-full h-32 bg-gray-100 rounded-lg flex items-center justify-center text-gray-400">
                        <el-icon><Picture /></el-icon>
                      </div>
                      <div class="text-xs text-gray-600 mt-1">{{ getEvidenceTypeText(ev.evidenceType) }}</div>
                      <div v-if="ev.description" class="text-xs text-gray-500 mt-1">{{ ev.description }}</div>
                    </div>
                  </div>
                </div>

                <div v-if="disputeInfo.status === 'pending' || disputeInfo.status === 'supervisor_submitted'" class="mt-4">
                  <el-button type="primary" @click="openWorkerSubmitDialog" class="rounded-lg">
                    {{ disputeInfo.workerSubmitTime ? '补充提交' : '提交说明和证据' }}
                  </el-button>
                </div>
              </div>
            </el-col>

            <el-col :span="12">
              <div class="party-section p-4 bg-blue-50 rounded-lg border border-blue-200">
                <div class="flex items-center justify-between mb-4">
                  <h4 class="font-bold text-lg text-blue-800">
                    <el-icon class="mr-1"><OfficeBuilding /></el-icon>
                    主管提交
                  </h4>
                  <el-tag v-if="disputeInfo.supervisorSubmitTime" type="success" size="small">已提交</el-tag>
                  <el-tag v-else type="info" size="small">待提交</el-tag>
                </div>
                <el-descriptions :column="1" size="small" border class="mb-4">
                  <el-descriptions-item label="提交时间">
                    {{ disputeInfo.supervisorSubmitTime || '未提交' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="主管ID">{{ disputeInfo.supervisorId || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="主管说明">
                    <div style="white-space: pre-wrap">{{ disputeInfo.supervisorRemark || '暂无说明' }}</div>
                  </el-descriptions-item>
                </el-descriptions>
                <div class="evidence-list">
                  <div class="text-sm font-medium mb-2 text-gray-700">主管提供的证据：</div>
                  <el-empty v-if="supervisorEvidences.length === 0" description="暂无证据" :image-size="60" />
                  <div v-else class="grid grid-cols-3 gap-3">
                    <div v-for="(ev, idx) in supervisorEvidences" :key="idx" class="evidence-item">
                      <el-image
                        v-if="ev.fileUrl"
                        :src="ev.fileUrl"
                        :preview-src-list="supervisorEvidences.filter(e => e.fileUrl).map(e => e.fileUrl)"
                        :initial-index="supervisorEvidences.findIndex(e => e.id === ev.id)"
                        fit="cover"
                        class="w-full h-32 rounded-lg border"
                      />
                      <div v-else class="w-full h-32 bg-gray-100 rounded-lg flex items-center justify-center text-gray-400">
                        <el-icon><Picture /></el-icon>
                      </div>
                      <div class="text-xs text-gray-600 mt-1">{{ getEvidenceTypeText(ev.evidenceType) }}</div>
                      <div v-if="ev.description" class="text-xs text-gray-500 mt-1">{{ ev.description }}</div>
                    </div>
                  </div>
                </div>

                <div v-if="disputeInfo.status === 'pending' || disputeInfo.status === 'worker_submitted'" class="mt-4">
                  <el-button type="primary" @click="openSupervisorSubmitDialog" class="rounded-lg">
                    {{ disputeInfo.supervisorSubmitTime ? '补充提交' : '提交说明和证据' }}
                  </el-button>
                </div>
              </div>
            </el-col>
          </el-row>

          <div v-if="arbitrationInfo" class="mt-6 p-4 bg-purple-50 rounded-lg border border-purple-200">
            <div class="flex items-center justify-between mb-4">
              <h4 class="font-bold text-lg text-purple-800">
                <el-icon class="mr-1"><Scale /></el-icon>
                仲裁结果
              </h4>
              <el-tag :type="getArbitrationResultType(arbitrationInfo.arbitrationResult)" size="large" class="rounded-full">
                {{ getArbitrationResultText(arbitrationInfo.arbitrationResult) }}
              </el-tag>
            </div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="仲裁人">{{ arbitrationInfo.arbitratorName }}</el-descriptions-item>
              <el-descriptions-item label="仲裁时间">{{ arbitrationInfo.arbitrationTime }}</el-descriptions-item>
              <el-descriptions-item label="裁定工时">{{ arbitrationInfo.approvedHours }} 小时</el-descriptions-item>
              <el-descriptions-item label="裁定金额">
                <span class="text-blue-600 font-bold">¥{{ arbitrationInfo.approvedAmount }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="仲裁意见" :span="2">
                <div style="white-space: pre-wrap">{{ arbitrationInfo.arbitrationOpinion }}</div>
              </el-descriptions-item>
              <el-descriptions-item v-if="arbitrationInfo.remark" label="备注" :span="2">
                {{ arbitrationInfo.remark }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="workerSubmitVisible" title="工人提交说明和证据" width="600px" class="rounded-lg">
      <el-form :model="workerSubmitForm" label-width="100px">
        <el-form-item label="工人ID">
          <el-input v-model="workerSubmitForm.workerId" />
        </el-form-item>
        <el-form-item label="工人说明">
          <el-input v-model="workerSubmitForm.workerRemark" type="textarea" :rows="4" placeholder="请输入详细说明" />
        </el-form-item>
        <el-form-item label="上传证据">
          <el-upload
            :action="uploadAction"
            list-type="picture-card"
            :on-success="handleWorkerUploadSuccess"
            :file-list="workerSubmitForm.evidenceFiles"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="text-xs text-gray-500 mt-2">支持上传照片、聊天记录截图等</div>
          <div v-if="workerSubmitForm.evidences.length > 0" class="mt-4 space-y-2">
            <div v-for="(ev, idx) in workerSubmitForm.evidences" :key="idx" class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
              <el-image :src="ev.fileUrl" class="w-16 h-16 rounded" fit="cover" />
              <div class="flex-1">
                <div class="text-sm text-gray-700 mb-1">{{ ev.fileName }}</div>
                <el-select v-model="ev.evidenceType" size="small" style="width: 140px">
                  <el-option label="照片" value="photo" />
                  <el-option label="聊天记录" value="chat" />
                  <el-option label="其他" value="other" />
                </el-option>
              </div>
              <el-input v-model="ev.description" size="small" placeholder="证据说明" style="width: 200px" />
              <el-button type="danger" size="small" @click="handleWorkerRemoveEvidence(idx)">删除</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="workerSubmitVisible = false" class="rounded-lg">取消</el-button>
        <el-button type="primary" @click="submitWorkerForm" class="rounded-lg" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="supervisorSubmitVisible" title="主管提交说明和证据" width="600px" class="rounded-lg">
      <el-form :model="supervisorSubmitForm" label-width="100px">
        <el-form-item label="主管ID">
          <el-input v-model="supervisorSubmitForm.supervisorId" />
        </el-form-item>
        <el-form-item label="主管姓名">
          <el-input v-model="supervisorSubmitForm.supervisorName" />
        </el-form-item>
        <el-form-item label="主管说明">
          <el-input v-model="supervisorSubmitForm.supervisorRemark" type="textarea" :rows="4" placeholder="请输入详细说明" />
        </el-form-item>
        <el-form-item label="上传证据">
          <el-upload
            :action="uploadAction"
            list-type="picture-card"
            :on-success="handleSupervisorUploadSuccess"
            :file-list="supervisorSubmitForm.evidenceFiles"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="text-xs text-gray-500 mt-2">支持上传照片、聊天记录截图等</div>
          <div v-if="supervisorSubmitForm.evidences.length > 0" class="mt-4 space-y-2">
            <div v-for="(ev, idx) in supervisorSubmitForm.evidences" :key="idx" class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
              <el-image :src="ev.fileUrl" class="w-16 h-16 rounded" fit="cover" />
              <div class="flex-1">
                <div class="text-sm text-gray-700 mb-1">{{ ev.fileName }}</div>
                <el-select v-model="ev.evidenceType" size="small" style="width: 140px">
                  <el-option label="照片" value="photo" />
                  <el-option label="聊天记录" value="chat" />
                  <el-option label="其他" value="other" />
                </el-option>
              </div>
              <el-input v-model="ev.description" size="small" placeholder="证据说明" style="width: 200px" />
              <el-button type="danger" size="small" @click="handleSupervisorRemoveEvidence(idx)">删除</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supervisorSubmitVisible = false" class="rounded-lg">取消</el-button>
        <el-button type="primary" @click="submitSupervisorForm" class="rounded-lg" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, User, OfficeBuilding, Picture, Scale } from '@element-plus/icons-vue'
import { disputeApi } from '@/api/dispute'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const disputeInfo = ref(null)
const arbitrationInfo = ref(null)
const evidences = ref([])
const workerSubmitVisible = ref(false)
const supervisorSubmitVisible = ref(false)

const uploadAction = `${import.meta.env.VITE_API_BASE_URL || '/api'}/file/upload`

const disputeId = computed(() => route.params.id)

const workerEvidences = computed(() => evidences.value.filter(e => e.submitterType === 'worker'))
const supervisorEvidences = computed(() => evidences.value.filter(e => e.submitterType === 'supervisor'))

const workerSubmitForm = reactive({
  workerId: '',
  workerRemark: '',
  evidenceFiles: [],
  evidences: []
})

const supervisorSubmitForm = reactive({
  supervisorId: '1',
  supervisorName: '主管',
  supervisorRemark: '',
  evidenceFiles: [],
  evidences: []
})

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await disputeApi.getById(disputeId.value)
    if (res.code === 200) {
      disputeInfo.value = res.data.dispute
      arbitrationInfo.value = res.data.arbitration
      evidences.value = res.data.evidences || []
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

const handleStartArbitration = async () => {
  try {
    await ElMessageBox.confirm('确定要启动仲裁吗？启动后将进入仲裁流程。', '提示', { type: 'warning' })
    const res = await disputeApi.startArbitration(disputeId.value)
    if (res.code === 200) {
      ElMessage.success('已启动仲裁')
      loadDetail()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('启动仲裁失败')
    }
  }
}

const goToArbitration = () => {
  router.push(`/pc/arbitration/${disputeId.value}`)
}

const openWorkerSubmitDialog = () => {
  workerSubmitForm.workerId = disputeInfo.value?.workerId || ''
  workerSubmitForm.workerRemark = disputeInfo.value?.workerRemark || ''
  workerSubmitForm.evidenceFiles = []
  workerSubmitForm.evidences = []
  workerSubmitVisible.value = true
}

const openSupervisorSubmitDialog = () => {
  supervisorSubmitForm.supervisorRemark = disputeInfo.value?.supervisorRemark || ''
  supervisorSubmitForm.evidenceFiles = []
  supervisorSubmitForm.evidences = []
  supervisorSubmitVisible.value = true
}

const handleWorkerUploadSuccess = (response, uploadFile) => {
  if (response.code === 200) {
    workerSubmitForm.evidences.push({
      evidenceType: 'photo',
      fileId: response.data.id,
      fileUrl: response.data.filePath,
      description: '',
      fileName: uploadFile.name
    })
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleSupervisorUploadSuccess = (response, uploadFile) => {
  if (response.code === 200) {
    supervisorSubmitForm.evidences.push({
      evidenceType: 'photo',
      fileId: response.data.id,
      fileUrl: response.data.filePath,
      description: '',
      fileName: uploadFile.name
    })
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleWorkerRemoveEvidence = (index) => {
  workerSubmitForm.evidences.splice(index, 1)
  workerSubmitForm.evidenceFiles.splice(index, 1)
}

const handleSupervisorRemoveEvidence = (index) => {
  supervisorSubmitForm.evidences.splice(index, 1)
  supervisorSubmitForm.evidenceFiles.splice(index, 1)
}

const submitWorkerForm = async () => {
  submitting.value = true
  try {
    const res = await disputeApi.workerSubmit(disputeId.value, {
      workerId: workerSubmitForm.workerId,
      workerRemark: workerSubmitForm.workerRemark,
      evidences: workerSubmitForm.evidences
    })
    if (res.code === 200) {
      ElMessage.success('提交成功')
      workerSubmitVisible.value = false
      loadDetail()
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

const submitSupervisorForm = async () => {
  submitting.value = true
  try {
    const res = await disputeApi.supervisorSubmit(disputeId.value, {
      supervisorId: supervisorSubmitForm.supervisorId,
      supervisorName: supervisorSubmitForm.supervisorName,
      supervisorRemark: supervisorSubmitForm.supervisorRemark,
      evidences: supervisorSubmitForm.evidences
    })
    if (res.code === 200) {
      ElMessage.success('提交成功')
      supervisorSubmitVisible.value = false
      loadDetail()
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

const getStatusType = (status) => {
  const types = {
    pending: 'info',
    worker_submitted: 'warning',
    supervisor_submitted: 'warning',
    arbitrating: 'primary',
    approved: 'success',
    rejected: 'danger',
    closed: 'info'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    pending: '待处理',
    worker_submitted: '工人已提交',
    supervisor_submitted: '主管已提交',
    arbitrating: '仲裁中',
    approved: '仲裁通过',
    rejected: '仲裁驳回',
    closed: '已关闭'
  }
  return texts[status] || status
}

const getEvidenceTypeText = (type) => {
  const texts = { photo: '照片', chat: '聊天记录', other: '其他' }
  return texts[type] || type
}

const getArbitrationResultType = (result) => {
  const types = { approved: 'success', rejected: 'danger', partial: 'warning' }
  return types[result] || 'info'
}

const getArbitrationResultText = (result) => {
  const texts = { approved: '通过', rejected: '驳回', partial: '部分支持' }
  return texts[result] || result
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.dispute-detail-page {
  max-width: 1400px;
  margin: 0 auto;
}

.evidence-item {
  cursor: pointer;
}
</style>
