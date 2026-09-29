export const FAN_FREQUENCY_MIN = 135
export const FAN_FREQUENCY_MAX = 200

export const isFanFrequencyInRange = (value) => {
  const frequency = Number(value)
  return Number.isFinite(frequency)
    && frequency >= FAN_FREQUENCY_MIN
    && frequency <= FAN_FREQUENCY_MAX
}
