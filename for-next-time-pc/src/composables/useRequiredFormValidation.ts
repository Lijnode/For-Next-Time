import { ref } from 'vue'

type RequiredField = {
  value: string | null | undefined
  message: string
}

/**
 * 管理桌面端表单的必填校验提示。
 */
export function useRequiredFormValidation() {
  const validationMessage = ref('')

  /**
   * 清除表单顶部已有的必填提示。
   */
  function clearRequiredValidation() {
    // 1. 在再次提交前移除已过期的提示。
    validationMessage.value = ''
  }

  /**
   * 查找首个未填写的字段，并将提示显示在表单顶部。
   *
   * @param fields 当前表单的必填字段及其提示文案
   */
  function validateRequired(fields: RequiredField[]) {
    // 1. 清理上一次的必填提示。
    clearRequiredValidation()
    // 2. 按页面字段顺序定位第一个为空或仅包含空格的字段。
    const missingField = fields.find((field) => !field.value?.trim())
    // 3. 有缺失字段时写入顶部提示并停止提交。
    if (missingField) {
      validationMessage.value = missingField.message
      return false
    }
    // 4. 所有必填项均已填写，允许页面继续业务校验。
    return true
  }

  return { validationMessage, clearRequiredValidation, validateRequired }
}
