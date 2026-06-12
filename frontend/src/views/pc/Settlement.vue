<template>
  <div class="settlement-page">
    <el-card class="mb-4 shadow-sm rounded-lg border-0" :body-style="{ padding: '24px' }">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-xl font-bold text-gray-800">日结工资单管理</span>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="mb-6">
        <el-form-item label="工人ID">
          <el-input v-model="searchForm.workerId" placeholder="请输入工人ID" clearable class="rounded-lg" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 180px" class="rounded-lg">
            <el-option label="待确认" value="pending" />
            <el-option label="已确认" value="confirmed" />
            <el-option label="有争议" value="disputed" />
            <el-option label="已打款" value="paid" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作日期">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" class="rounded-lg">查询</el-button>
          <el-button @click="handleReset" class="rounded-lg">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border class="rounded-lg overflow-hidden" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="settlementNo" label="结算单号" min-width="180" />
        <el-table-column label="工人信息" width="160">
          <template #default="{ row }">
            <div class="font-medium">{{ row.workerName || '-' }}</div>
            <div class="text-xs text-gray-500">工号：{{ row.workerNo || '-' }}</div>
            <div class="text-xs text-gray-500">岗位：{{ row.positionName || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="workDate" label="工作日期" width="120" />
        <el-table-column label="工时" width="110">
          <template #default="{ row }">
            <div>原始：{{ row.originalHours }}h</div>
            <div class="text-blue-600">结算：{{ row.actualHours }}h</div>
          </template>
        </el-table-column>
        <el-table-column label="工资明细" width="160">
          <template #default="{ row }">
            <div>基础：¥{{ row.baseAmount }}</div>
            <div class="text-green-600">补贴：¥{{ row.tempSubsidy }}</div>
            <div class="text-red-600">扣款：¥{{ row.deductionAmount }}</div>
            <div class="font-bold text-orange-600 pt-1 border-t mt-1">应发：¥{{ row.totalAmount }}</div>
          </template>
        </el-table-column>
        <el-table-column label="争议原因" min-width="180">
          <template #default="{ row }">
            <div v-if="row.dispute">
              <el-tag type="warning" size="small" class="mb-1">有争议</el-tag>
              <div class="text-xs text-red-600 font-medium">{{ row.disputeReason || '暂无' }}</div>
              <div class="text-xs text-gray-500">
                类型：{{ row.dispute.disputeType === 'supervisor_deduction' ? '主管扣时' : '工人申诉' }}
              </div>
              <div class="text-xs text-gray-500">
                状态：{{ getDisputeStatusText(row.dispute.status) }}
              </div>
            </div>
            <el-tag v-else type="success" size="small">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="结算状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" class="rounded-full px-3 py-1">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleView(row)">查看详情</el-button>
            <el-button v-if="row.status === 'pending'" type="success" size="small" link @click="handleConfirm(row)">确认</el-button>
            <el-button v-if="row.status === 'pending'" type="warning" size="small" link @click="handleDeduction(row)">主管扣时</el-button>
            <el-button v-if="row.status === 'pending'" type="danger" size="small" link @click="handleAppeal(row)">工人申诉</el-button>
            <el-button v-if="row.status === 'disputed' && row.disputeId" type="info" size="small" link @click="handleViewDispute(row)">处理争议</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-6 flex justify-end">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="rounded-lg"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="日结工资单详情" width="700px" class="rounded-lg">
      <div v-if="currentDetail" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="结算单号">{{ currentDetail.settlement.settlementNo }}</el-descriptions-item>
          <el-descriptions-item label="工作日期">{{ currentDetail.settlement.workDate }}</el-descriptions-item>
          <el-descriptions-item label="工人">{{ currentDetail.worker?.workerName }} ({{ currentDetail.worker?.workerNo }})</el-descriptions-item>
          <el-descriptions-item label="岗位">{{ currentDetail.position?.positionName }}</el-descriptions-item>
          <el-descriptions-item label="原始工时">{{ currentDetail.settlement.originalHours }} 小时</el-descriptions-item>
          <el-descriptions-item label="结算工时">{{ currentDetail.settlement.actualHours }} 小时</el-descriptions-item>
          <el-descriptions-item label="岗位单价">¥{{ currentDetail.settlement.unitPrice }}/时</el-descriptions-item>
          <el-descriptions-item label="基础工资">¥{{ currentDetail.settlement.baseAmount }}</el-descriptions-item>
          <el-descriptions-item label="临时补贴">¥{{ currentDetail.settlement.tempSubsidy }}</el-descriptions-item>
          <el-descriptions-item label="扣款金额">¥{{ currentDetail.settlement.deductionAmount }}</el-descriptions-item>
          <el-descriptions-item label="应发工资" :span="2">
            <span class="text-xl font-bold text-orange-600">¥{{ currentDetail.settlement.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentDetail.settlement.status)">{{ getStatusText(currentDetail.settlement.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注">{{ currentDetail.settlement.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentDetail.dispute" class="mt-6">
          <h4 class="font-bold text-lg mb-3 text-red-600">争议信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="争议单号">{{ currentDetail.dispute.disputeNo }}</el-descriptions-item>
            <el-descriptions-item label="争议类型">
              {{ currentDetail.dispute.disputeType === 'supervisor_deduction' ? '主管扣时' : '工人申诉' }}
            </el-descriptions-item>
            <el-descriptions-item label="原始金额">¥{{ currentDetail.dispute.originalAmount }}</el-descriptions-item>
            <el-descriptions-item label="主张金额">¥{{ currentDetail.dispute.claimedAmount }}</el-descriptions-item>
            <el-descriptions-item label="争议原因" :span="2">{{ currentDetail.dispute.disputeReason }}</el-descriptions-item>
            <el-descriptions-item label="争议状态">
              <el-tag type="warning">{{ getDisputeStatusText(currentDetail.dispute.status) }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <el-button v-if="currentDetail.arbitration" type="primary" link class="mt-3" @click="viewArbitrationDetail(currentDetail)">查看仲裁结果</el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false" class="rounded-lg">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deductionVisible" title="主管扣时" width="600px" class="rounded-lg">
      <el-form :model="deductionForm" :rules="deductionRules" ref="deductionFormRef" label-width="100px">
        <el-form-item label="主张工时" prop="claimedHours">
          <el-input-number v-model="deductionForm.claimedHours" :min="0" :max="24" :step="0.5" /> 小时
        </el-form-item>
        <el-form-item label="主张金额" prop="claimedAmount">
          <el-input-number v-model="deductionForm.claimedAmount" :min="0" :precision="2" :step="1" /> 元
        </el-form-item>
        <el-form-item label="争议原因" prop="disputeReason">
          <el-input v-model="deductionForm.disputeReason" type="textarea" :rows="3" placeholder="请输入扣时原因，如：迟到早退、未完成工作等" />
        </el-form-item>
        <el-form-item label="主管说明" prop="supervisorRemark">
          <el-input v-model="deductionForm.supervisorRemark" type="textarea" :rows="3" placeholder="请输入详细说明" />
        </el-form-item>
        <el-form-item label="上传证据">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            list-type="picture-card"
            :on-success="handleDeductionUploadSuccess"
            :file-list="deductionForm.evidenceFiles"
            :auto-upload="true"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="text-xs text-gray-500 mt-2">支持上传照片、聊天记录截图等证据</div>
          <div v-if="deductionForm.evidences.length > 0" class="mt-4 space-y-2">
            <div v-for="(ev, idx) in deductionForm.evidences" :key="idx" class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
              <el-image :src="ev.fileUrl" class="w-16 h-16 rounded" fit="cover" />
              <div class="flex-1">
                <div class="text-sm text-gray-700 mb-1">{{ ev.fileName }}</div>
                <el-select v-model="ev.evidenceType" size="small" style="width: 140px">
                  <el-option label="照片" value="photo" />
                  <el-option label="聊天记录" value="chat" />
                  <el-option label="其他" value="other" />
                </el-select>
              </div>
              <el-input v-model="ev.description" size="small" placeholder="证据说明" style="width: 200px" />
              <el-button type="danger" size="small" @click="handleDeductionRemoveEvidence(idx)">删除</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deductionVisible = false" class="rounded-lg">取消</el-button>
        <el-button type="primary" @click="submitDeduction" class="rounded-lg" :loading="submitting">提交扣时</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="appealVisible" title="工人申诉" width="600px" class="rounded-lg">
      <el-form :model="appealForm" :rules="appealRules" ref="appealFormRef" label-width="100px">
        <el-form-item label="工人ID" prop="workerId">
          <el-input v-model="appealForm.workerId" />
        </el-form-item>
        <el-form-item label="主张工时" prop="claimedHours">
          <el-input-number v-model="appealForm.claimedHours" :min="0" :max="24" :step="0.5" /> 小时
        </el-form-item>
        <el-form-item label="主张金额" prop="claimedAmount">
          <el-input-number v-model="appealForm.claimedAmount" :min="0" :precision="2" :step="1" /> 元
        </el-form-item>
        <el-form-item label="争议原因" prop="disputeReason">
          <el-input v-model="appealForm.disputeReason" type="textarea" :rows="3" placeholder="请输入申诉原因，如：工时计算错误、应得补贴未发等" />
        </el-form-item>
        <el-form-item label="工人说明" prop="workerRemark">
          <el-input v-model="appealForm.workerRemark" type="textarea" :rows="3" placeholder="请输入详细说明" />
        </el-form-item>
        <el-form-item label="上传证据">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            list-type="picture-card"
            :on-success="handleAppealUploadSuccess"
            :file-list="appealForm.evidenceFiles"
            :auto-upload="true"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="text-xs text-gray-500 mt-2">支持上传照片、聊天记录截图等证据</div>
          <div v-if="appealForm.evidences.length > 0" class="mt-4 space-y-2">
            <div v-for="(ev, idx) in appealForm.evidences" :key="idx" class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
              <el-image :src="ev.fileUrl" class="w-16 h-16 rounded" fit="cover" />
              <div class="flex-1">
                <div class="text-sm text-gray-700 mb-1">{{ ev.fileName }}</div>
                <el-select v-model="ev.evidenceType" size="small" style="width: 140px">
                  <el-option label="照片" value="photo" />
                  <el-option label="聊天记录" value="chat" />
                  <el-option label="其他" value="other" />
                </el-select>
              </div>
              <el-input v-model="ev.description" size="small" placeholder="证据说明" style="width: 200px" />
              <el-button type="danger" size="small" @click="handleAppealRemoveEvidence(idx)">删除</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealVisible = false" class="rounded-lg">取消</el-button>
        <el-button type="primary" @click="submitAppeal" class="rounded-lg" :loading="submitting">提交申诉</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { settlementApi } from '@/api/settlement'
import { disputeApi } from '@/api/dispute'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const deductionVisible = ref(false)
const appealVisible = ref(false)
const currentDetail = ref(null)
const currentSettlement = ref(null)
const deductionFormRef = ref(null)
const appealFormRef = ref(null)

const uploadAction = `${import.meta.env.VITE_API_BASE_URL || '/api'}/file/upload`
const uploadHeaders = {}

const searchForm = reactive({
  workerId: '',
  status: '',
  dateRange: []
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const deductionForm = reactive({
  claimedHours: 0,
  claimedAmount: 0,
  disputeReason: '',
  supervisorRemark: '',
  evidenceFiles: [],
  evidences: []
})

const appealForm = reactive({
  workerId: '',
  claimedHours: 0,
  claimedAmount: 0,
  disputeReason: '',
  workerRemark: '',
  evidenceFiles: [],
  evidences: []
})

const deductionRules = {
  claimedHours: [{ required: true, message: '请输入主张工时', trigger: 'blur' }],
  claimedAmount: [{ required: true, message: '请输入主张金额', trigger: 'blur' }],
  disputeReason: [{ required: true, message: '请输入争议原因', trigger: 'blur' }]
}

const appealRules = {
  workerId: [{ required: true, message: '请输入工人ID', trigger: 'blur' }],
  claimedHours: [{ required: true, message: '请输入主张工时', trigger: 'blur' }],
  claimedAmount: [{ required: true, message: '请输入主张金额', trigger: 'blur' }],
  disputeReason: [{ required: true, message: '请输入争议原因', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size
    }
    if (searchForm.workerId) params.workerId = searchForm.workerId
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.workDateStart = searchForm.dateRange[0]
      params.workDateEnd = searchForm.dateRange[1]
    }
    const res = await settlementApi.pageWithDetail(params)
    if (res.code === 200) {
      tableData.value = res.data.records.map(item => ({
        ...item.settlement,
        workerName: item.worker?.workerName,
        workerNo: item.worker?.workerNo,
        positionName: item.position?.positionName,
        dispute: item.dispute,
        disputeReason: item.dispute?.disputeReason,
        arbitration: item.arbitration
      }))
      pagination.total = res.data.total
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  Object.assign(searchForm, { workerId: '', status: '', dateRange: [] })
  handleSearch()
}

const handleView = async (row) => {
  try {
    const res = await settlementApi.getById(row.id)
    if (res.code === 200) {
      currentDetail.value = res.data
      detailVisible.value = true
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取详情失败')
  }
}

const handleConfirm = async (row) => {
  try {
    await ElMessageBox.confirm('确定要确认该结算单吗？', '提示', { type: 'warning' })
    const res = await settlementApi.confirm(row.id)
    if (res.code === 200) {
      ElMessage.success('确认成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error('确认失败')
    }
  }
}

const handleDeduction = (row) => {
  currentSettlement.value = row
  Object.assign(deductionForm, {
    claimedHours: row.originalHours ? row.originalHours - 1 : 0,
    claimedAmount: row.totalAmount ? row.totalAmount - row.unitPrice : 0,
    disputeReason: '',
    supervisorRemark: '',
    evidenceFiles: [],
    evidences: []
  })
  deductionVisible.value = true
}

const handleAppeal = (row) => {
  currentSettlement.value = row
  Object.assign(appealForm, {
    workerId: row.workerId,
    claimedHours: row.originalHours,
    claimedAmount: row.totalAmount,
    disputeReason: '',
    workerRemark: '',
    evidenceFiles: [],
    evidences: []
  })
  appealVisible.value = true
}

const handleDeductionUploadSuccess = (response, uploadFile) => {
  if (response.code === 200) {
    deductionForm.evidences.push({
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

const handleAppealUploadSuccess = (response, uploadFile) => {
  if (response.code === 200) {
    appealForm.evidences.push({
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

const handleDeductionRemoveEvidence = (index) => {
  deductionForm.evidences.splice(index, 1)
  deductionForm.evidenceFiles.splice(index, 1)
}

const handleAppealRemoveEvidence = (index) => {
  appealForm.evidences.splice(index, 1)
  appealForm.evidenceFiles.splice(index, 1)
}

const submitDeduction = async () => {
  if (!deductionFormRef.value) return
  await deductionFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const res = await disputeApi.createSupervisorDeduction({
          settlementId: currentSettlement.value.id,
          claimedHours: deductionForm.claimedHours,
          claimedAmount: deductionForm.claimedAmount,
          disputeReason: deductionForm.disputeReason,
          supervisorRemark: deductionForm.supervisorRemark,
          evidences: deductionForm.evidences
        })
        if (res.code === 200) {
          ElMessage.success('发起扣时成功')
          deductionVisible.value = false
          loadData()
        }
      } catch (error) {
        console.error(error)
        ElMessage.error('提交失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const submitAppeal = async () => {
  if (!appealFormRef.value) return
  await appealFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const res = await disputeApi.createWorkerAppeal({
          settlementId: currentSettlement.value.id,
          workerId: appealForm.workerId,
          claimedHours: appealForm.claimedHours,
          claimedAmount: appealForm.claimedAmount,
          disputeReason: appealForm.disputeReason,
          workerRemark: appealForm.workerRemark,
          evidences: appealForm.evidences
        })
        if (res.code === 200) {
          ElMessage.success('发起申诉成功')
          appealVisible.value = false
          loadData()
        }
      } catch (error) {
        console.error(error)
        ElMessage.error('提交失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleViewDispute = (row) => {
  router.push(`/pc/dispute/${row.disputeId}`)
}

const viewArbitrationDetail = (detail) => {
  router.push(`/pc/arbitration/${detail.dispute.id}`)
}

const getStatusType = (status) => {
  const types = {
    pending: 'info',
    confirmed: 'success',
    disputed: 'warning',
    paid: 'success',
    cancelled: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    pending: '待确认',
    confirmed: '已确认',
    disputed: '有争议',
    paid: '已打款',
    cancelled: '已取消'
  }
  return texts[status] || status
}

const getDisputeStatusText = (status) => {
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

const handleSizeChange = (size) => {
  pagination.size = size
  loadData()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.settlement-page {
  max-width: 1600px;
  margin: 0 auto;
}

.detail-content :deep(.el-descriptions__label) {
  width: 120px;
}
</style>
