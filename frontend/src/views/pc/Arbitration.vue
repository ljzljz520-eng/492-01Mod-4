<template>
  <div class="arbitration-page">
    <el-card class="mb-4 shadow-sm rounded-lg border-0" :body-style="{ padding: '24px' }">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <el-button @click="goBack" class="rounded-lg">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <span class="text-xl font-bold text-gray-800">争议仲裁</span>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <div v-if="disputeInfo">
          <h3 class="text-lg font-bold mb-4">争议信息</h3>
          <el-descriptions :column="2" border class="mb-6">
            <el-descriptions-item label="争议单号">{{ disputeInfo.disputeNo }}</el-descriptions-item>
            <el-descriptions-item label="争议类型">
              {{ disputeInfo.disputeType === 'supervisor_deduction' ? '主管扣时' : '工人申诉' }}
            </el-descriptions-item>
            <el-descriptions-item label="原始工时/金额">
              {{ disputeInfo.originalHours }} 小时 / ¥{{ disputeInfo.originalAmount }}
            </el-descriptions-item>
            <el-descriptions-item label="主张工时/金额">
              {{ disputeInfo.claimedHours }} 小时 / ¥{{ disputeInfo.claimedAmount }}
            </el-descriptions-item>
            <el-descriptions-item label="争议原因" :span="2">
              <span class="text-red-600">{{ disputeInfo.disputeReason }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="工人说明">
              <div style="white-space: pre-wrap">{{ disputeInfo.workerRemark || '暂无' }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="主管说明">
              <div style="white-space: pre-wrap">{{ disputeInfo.supervisorRemark || '暂无' }}</div>
            </el-descriptions-item>
          </el-descriptions>

          <h3 class="text-lg font-bold mb-4">双方证据</h3>
          <el-row :gutter="20" class="mb-6">
            <el-col :span="12">
              <div class="p-4 bg-orange-50 rounded-lg border border-orange-200">
                <div class="font-bold text-orange-800 mb-3">工人证据</div>
                <el-empty v-if="workerEvidences.length === 0" description="暂无证据" :image-size="60" />
                <div v-else class="grid grid-cols-3 gap-3">
                  <div v-for="(ev, idx) in workerEvidences" :key="idx">
                    <el-image
                      v-if="ev.fileUrl"
                      :src="ev.fileUrl"
                      :preview-src-list="workerEvidences.filter(e => e.fileUrl).map(e => e.fileUrl)"
                      fit="cover"
                      class="w-full h-32 rounded-lg border"
                    />
                    <div v-if="ev.description" class="text-xs text-gray-500 mt-1">{{ ev.description }}</div>
                  </div>
                </div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="p-4 bg-blue-50 rounded-lg border border-blue-200">
                <div class="font-bold text-blue-800 mb-3">主管证据</div>
                <el-empty v-if="supervisorEvidences.length === 0" description="暂无证据" :image-size="60" />
                <div v-else class="grid grid-cols-3 gap-3">
                  <div v-for="(ev, idx) in supervisorEvidences" :key="idx">
                    <el-image
                      v-if="ev.fileUrl"
                      :src="ev.fileUrl"
                      :preview-src-list="supervisorEvidences.filter(e => e.fileUrl).map(e => e.fileUrl)"
                      fit="cover"
                      class="w-full h-32 rounded-lg border"
                    />
                    <div v-if="ev.description" class="text-xs text-gray-500 mt-1">{{ ev.description }}</div>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>

          <div v-if="arbitrationInfo" class="mb-6 p-4 bg-purple-50 rounded-lg border border-purple-200">
            <div class="font-bold text-purple-800 mb-3">已存在仲裁结果</div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="仲裁结果">
                <el-tag :type="getArbitrationResultType(arbitrationInfo.arbitrationResult)">
                  {{ getArbitrationResultText(arbitrationInfo.arbitrationResult) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="仲裁人">{{ arbitrationInfo.arbitratorName }}</el-descriptions-item>
              <el-descriptions-item label="裁定工时">{{ arbitrationInfo.approvedHours }} 小时</el-descriptions-item>
              <el-descriptions-item label="裁定金额">¥{{ arbitrationInfo.approvedAmount }}</el-descriptions-item>
              <el-descriptions-item label="仲裁意见" :span="2">
                <div style="white-space: pre-wrap">{{ arbitrationInfo.arbitrationOpinion }}</div>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div v-if="disputeInfo.status === 'arbitrating' && !arbitrationInfo" class="p-4 bg-gray-50 rounded-lg border">
            <h3 class="text-lg font-bold mb-4">执行仲裁</h3>
            <el-form :model="arbitrationForm" :rules="arbitrationRules" ref="arbitrationFormRef" label-width="120px">
              <el-form-item label="仲裁员姓名">
                <el-input v-model="arbitrationForm.arbitratorName" />
              </el-form-item>
              <el-form-item label="仲裁结果" prop="arbitrationResult">
                <el-radio-group v-model="arbitrationForm.arbitrationResult">
                  <el-radio label="approved">通过（按主张金额）</el-radio>
                  <el-radio label="partial">部分支持</el-radio>
                  <el-radio label="rejected">驳回（按原始金额）</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="裁定工时" prop="approvedHours">
                <el-input-number v-model="arbitrationForm.approvedHours" :min="0" :max="24" :step="0.5" /> 小时
              </el-form-item>
              <el-form-item label="裁定金额" prop="approvedAmount">
                <el-input-number v-model="arbitrationForm.approvedAmount" :min="0" :precision="2" :step="1" /> 元
              </el-form-item>
              <el-form-item label="仲裁意见" prop="arbitrationOpinion">
                <el-input
                  v-model="arbitrationForm.arbitrationOpinion"
                  type="textarea"
                  :rows="5"
                  placeholder="请详细说明仲裁理由和依据"
                />
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="arbitrationForm.remark" type="textarea" :rows="2" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitArbitration" class="rounded-lg" :loading="submitting">
                  提交仲裁结果
                </el-button>
                <el-alert
                  title="仲裁结果一旦提交将不可撤销，结算单金额会自动更新并进入打款流程。"
                  type="warning"
                  :closable="false"
                  class="mt-2"
                  show-icon
                />
              </el-form-item>
            </el-form>
          </div>

          <div v-if="disputeInfo.status !== 'arbitrating'" class="text-center py-8">
            <el-empty :description="'当前状态：' + getStatusText(disputeInfo.status) + '，无法进行仲裁'" />
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { disputeApi } from '@/api/dispute'
import { arbitrationApi } from '@/api/arbitration'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const disputeInfo = ref(null)
const arbitrationInfo = ref(null)
const evidences = ref([])
const arbitrationFormRef = ref(null)

const disputeId = computed(() => route.params.id)
const workerEvidences = computed(() => evidences.value.filter(e => e.submitterType === 'worker'))
const supervisorEvidences = computed(() => evidences.value.filter(e => e.submitterType === 'supervisor'))

const arbitrationForm = reactive({
  arbitratorId: 1,
  arbitratorName: '仲裁员',
  arbitrationResult: '',
  approvedHours: 0,
  approvedAmount: 0,
  arbitrationOpinion: '',
  remark: ''
})

const arbitrationRules = {
  arbitrationResult: [{ required: true, message: '请选择仲裁结果', trigger: 'change' }],
  approvedHours: [{ required: true, message: '请输入裁定工时', trigger: 'blur' }],
  approvedAmount: [{ required: true, message: '请输入裁定金额', trigger: 'blur' }],
  arbitrationOpinion: [{ required: true, message: '请输入仲裁意见', trigger: 'blur' }]
}

watch(() => arbitrationForm.arbitrationResult, (val) => {
  if (!disputeInfo.value) return
  if (val === 'approved') {
    arbitrationForm.approvedHours = disputeInfo.value.claimedHours
    arbitrationForm.approvedAmount = disputeInfo.value.claimedAmount
  } else if (val === 'rejected') {
    arbitrationForm.approvedHours = disputeInfo.value.originalHours
    arbitrationForm.approvedAmount = disputeInfo.value.originalAmount
  }
})

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await disputeApi.getById(disputeId.value)
    if (res.code === 200) {
      disputeInfo.value = res.data.dispute
      arbitrationInfo.value = res.data.arbitration
      evidences.value = res.data.evidences || []
      if (disputeInfo.value && arbitrationForm.approvedHours === 0) {
        arbitrationForm.approvedHours = disputeInfo.value.originalHours
        arbitrationForm.approvedAmount = disputeInfo.value.originalAmount
      }
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

const submitArbitration = async () => {
  if (!arbitrationFormRef.value) return
  await arbitrationFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await ElMessageBox.confirm(
          `确认提交仲裁结果吗？\n裁定工时：${arbitrationForm.approvedHours}小时\n裁定金额：¥${arbitrationForm.approvedAmount}`,
          '确认仲裁',
          { type: 'warning' }
        )
        submitting.value = true
        const res = await arbitrationApi.doArbitration({
          disputeId: disputeId.value,
          ...arbitrationForm
        })
        if (res.code === 200) {
          ElMessage.success('仲裁完成')
          loadDetail()
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error(error)
          ElMessage.error('仲裁提交失败')
        }
      } finally {
        submitting.value = false
      }
    }
  })
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
.arbitration-page {
  max-width: 1200px;
  margin: 0 auto;
}
</style>
