<template>
  <div class="dispute-list-page">
    <el-card class="mb-4 shadow-sm rounded-lg border-0" :body-style="{ padding: '24px' }">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-xl font-bold text-gray-800">工资争议列表</span>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="mb-6">
        <el-form-item label="工人ID">
          <el-input v-model="searchForm.workerId" placeholder="请输入工人ID" clearable class="rounded-lg" />
        </el-form-item>
        <el-form-item label="争议状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 180px" class="rounded-lg">
            <el-option label="待处理" value="pending" />
            <el-option label="工人已提交" value="worker_submitted" />
            <el-option label="主管已提交" value="supervisor_submitted" />
            <el-option label="仲裁中" value="arbitrating" />
            <el-option label="仲裁通过" value="approved" />
            <el-option label="仲裁驳回" value="rejected" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item label="争议类型">
          <el-select v-model="searchForm.disputeType" placeholder="请选择" clearable style="width: 180px" class="rounded-lg">
            <el-option label="主管扣时" value="supervisor_deduction" />
            <el-option label="工人申诉" value="worker_appeal" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" class="rounded-lg">查询</el-button>
          <el-button @click="handleReset" class="rounded-lg">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border class="rounded-lg overflow-hidden" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="disputeNo" label="争议单号" min-width="200" />
        <el-table-column prop="settlementId" label="关联结算单ID" width="140" />
        <el-table-column prop="workerId" label="工人ID" width="100" />
        <el-table-column prop="disputeType" label="争议类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.disputeType === 'supervisor_deduction' ? 'warning' : 'danger'">
              {{ row.disputeType === 'supervisor_deduction' ? '主管扣时' : '工人申诉' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额对比" width="220">
          <template #default="{ row }">
            <div class="text-sm">
              <span class="text-gray-500">原始：</span><span class="font-medium">¥{{ row.originalAmount }}</span>
              <el-icon><ArrowRight /></el-icon>
              <span class="text-gray-500">主张：</span><span class="font-medium text-orange-600">¥{{ row.claimedAmount }}</span>
            </div>
            <div v-if="row.finalAmount !== null" class="text-sm mt-1">
              <span class="text-gray-500">最终：</span><span class="font-bold text-blue-600">¥{{ row.finalAmount }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="disputeReason" label="争议原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" class="rounded-full px-3 py-1">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleView(row)">处理详情</el-button>
            <el-button
              v-if="row.status === 'arbitrating'"
              type="warning"
              size="small"
              link
              @click="handleArbitration(row)"
            >
              进入仲裁
            </el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { disputeApi } from '@/api/dispute'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])

const searchForm = reactive({
  workerId: '',
  status: '',
  disputeType: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size
    }
    if (searchForm.workerId) params.workerId = searchForm.workerId
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.disputeType) params.disputeType = searchForm.disputeType
    const res = await disputeApi.page(params)
    if (res.code === 200) {
      tableData.value = res.data.records
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
  Object.assign(searchForm, { workerId: '', status: '', disputeType: '' })
  handleSearch()
}

const handleView = (row) => {
  router.push(`/pc/dispute/${row.id}`)
}

const handleArbitration = (row) => {
  router.push(`/pc/arbitration/${row.id}`)
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
.dispute-list-page {
  max-width: 1600px;
  margin: 0 auto;
}
</style>
