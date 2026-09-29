const screenDeviceCodes = new Set([
  'CS1',
  'CS2',
  'CS_CONVEYOR',
  'FS1',
  'FS2',
  'FS_CONVEYOR',
])

const parsePlcBit = (value) => {
  if (value === true || value === 1 || value === '1' || value === 'true') return true
  if (value === false || value === 0 || value === '0' || value === 'false') return false
  return null
}

export const normalizePlcBit = (value) => parsePlcBit(value) === true

export const isScreenDevice = (code) => screenDeviceCodes.has(String(code || '').toUpperCase())

export const readManualMode = (code, manualModeBit) => {
  const bitEnabled = parsePlcBit(manualModeBit)
  if (bitEnabled === null) return false
  return isScreenDevice(code) ? bitEnabled : !bitEnabled
}

export const manualModeWriteValue = (code, currentManualMode) => (
  isScreenDevice(code) ? !Boolean(currentManualMode) : Boolean(currentManualMode)
)

export const canControlInRemoteManual = (device) => (
  Boolean(device?.remote) && Boolean(device?.manualMode)
)
