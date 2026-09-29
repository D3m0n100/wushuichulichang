export const DEO_FAN_FREQUENCY_MIN = 0
export const DEO_FAN_FREQUENCY_MAX = 50

export const isDeodorizationFanFrequencyInRange = (value) => {
  const frequency = Number(value)
  return Number.isFinite(frequency)
    && frequency >= DEO_FAN_FREQUENCY_MIN
    && frequency <= DEO_FAN_FREQUENCY_MAX
}
