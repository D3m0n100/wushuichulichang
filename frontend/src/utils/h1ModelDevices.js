export const H1_MODEL_DEVICE_TARGETS = [
  { key: 'H1_QM2', targets: [{ name: '2#进水阀' }] },
  { key: 'H1_QM5', targets: [{ name: '5#进水阀' }] },
  { key: 'H1_HW1', targets: [{ name: '1#搅拌机电机' }] },
  { key: 'H1_HW2', targets: [{ name: '2#搅拌机电机' }] },
  { key: 'H1_HW3', targets: [{ name: '3#搅拌机', materialNames: ['金属'] }] },
  { key: 'H1_HW4', targets: [{ name: '4#搅拌机', materialNames: ['金属'] }] },
  { key: 'H1_HW5', targets: [{ name: '5#搅拌机电机' }] },
  { key: 'H1_HW6', targets: [{ name: '6#搅拌机电机' }] },
  { key: 'H1_HW7', targets: [{ name: '7#搅拌机电机' }] },
  { key: 'H1_HW8', targets: [{ name: '8#搅拌机电机' }] },
  { key: 'H1_QM3', targets: [{ name: '1#超越阀' }] },
  { key: 'H1_QM4', targets: [{ name: '2#超越阀' }] },
  { key: 'H1_GP1', targets: [{ name: '1#搅拌机', materialNames: ['Material #2'] }] },
  { key: 'H1_GP2', targets: [{ name: '2#搅拌机', materialNames: ['Material #2'] }] },
  { key: 'H1_GP3', targets: [{ name: '3#搅拌机', materialNames: ['Material #2'] }] },
  { key: 'H1_GP4', targets: [{ name: '4#搅拌机', materialNames: ['Material #2'] }] },
  { key: 'H1_QM1', targets: [{ name: '2号曝气阀' }] },
  { key: 'H1_QM10', targets: [{ name: '2#曝气阀' }] },
  { key: 'H1_QM6', targets: [{ name: '1#沉淀池出水阀' }] },
  { key: 'H1_QM7', targets: [{ name: '2#沉淀池出水阀' }] },
  { key: 'H1_QM8', targets: [{ name: '3#沉淀池出水阀' }] },
  { key: 'H1_QM9', targets: [{ name: '4#沉淀池出水阀' }] },
  { key: 'H1_GP5', targets: [{ name: '5#污泥回流泵' }] },
  { key: 'H1_GP6', targets: [{ name: '6#污泥回流泵' }] },
  { key: 'H1_GP7', targets: [{ name: '7#污泥回流泵' }] },
  { key: 'H1_GP8', targets: [{ name: '8#剩余污泥回流泵' }] },
  { key: 'H1_GP9', targets: [{ name: '9#剩余污泥回流泵' }] },
]

export const H1_DEVICE_STATE_STALE_MS = 8000

export const normalizeH1ModelMeshName = (name = '') => String(name)
  .trim()
  .replace(/\s+\(\d+\)$/u, '')

const matchesTarget = (descriptor, target) => {
  if (normalizeH1ModelMeshName(descriptor.name) !== normalizeH1ModelMeshName(target.name)) return false
  if (!target.materialNames?.length) return true
  return target.materialNames.includes(descriptor.materialName)
}

export const buildH1ModelMeshBindings = (meshDescriptors) => {
  const bindings = new Map()
  const issues = []
  const assignments = new Map()

  H1_MODEL_DEVICE_TARGETS.forEach(({ key, targets }) => {
    const resolved = []
    targets.forEach((target) => {
      const matches = meshDescriptors.filter(descriptor => matchesTarget(descriptor, target))
      if (matches.length !== 1) {
        issues.push(`${key} 的模型 ${target.name} 匹配到 ${matches.length} 个 mesh`)
        return
      }
      resolved.push(matches[0])
      if (!assignments.has(matches[0].id)) assignments.set(matches[0].id, new Set())
      assignments.get(matches[0].id).add(key)
    })
    bindings.set(key, resolved)
  })

  assignments.forEach((keys, meshId) => {
    if (keys.size <= 1) return
    const keyList = [...keys]
    issues.push(`mesh ${meshId} 被多个设备重复绑定：${keyList.join('、')}`)
    keyList.forEach((key) => {
      bindings.set(key, (bindings.get(key) || []).filter(descriptor => descriptor.id !== meshId))
    })
  })

  return { bindings, issues }
}

export const getH1ModelDeviceStatus = (device, fresh) => {
  if (!fresh || !device) return 'stopped'
  if (device.fault) return 'fault'
  if (device.type === 'valve') return device.open ? 'running' : 'stopped'
  return device.run ? 'running' : 'stopped'
}

export const isH1RealtimeDeviceStateFresh = ({
  connected,
  read,
  lastSuccessAt,
  now = Date.now(),
  staleMs = H1_DEVICE_STATE_STALE_MS,
}) => Boolean(
  connected
  && read
  && lastSuccessAt > 0
  && now - lastSuccessAt <= staleMs
)
