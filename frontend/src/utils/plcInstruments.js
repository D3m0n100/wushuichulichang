export const readInstrumentOutput = (instruments, code) => {
  if (!Array.isArray(instruments)) return undefined
  return instruments.find(instrument => instrument?.code === code)?.output
}

export const readPlcRealtimeValue = (overview, code) => {
  const requestedCode = String(code || '')
  const instrumentCode = requestedCode.endsWith('_OUTPUT')
    ? requestedCode.slice(0, -'_OUTPUT'.length)
    : requestedCode
  const instrumentOutput = readInstrumentOutput(overview?.instruments, instrumentCode)
  if (instrumentOutput !== undefined && instrumentOutput !== null) return instrumentOutput

  if (!Array.isArray(overview?.parameters)) return undefined
  const parameterCodes = new Set([
    requestedCode,
    instrumentCode,
    `${instrumentCode}_OUTPUT`,
  ])
  return overview.parameters.find(parameter => parameterCodes.has(parameter?.code))?.value
}
