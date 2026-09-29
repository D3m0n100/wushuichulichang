export const shouldRunH1Aeration = ({
  fanStatusFresh,
  anyFanRunning,
  qm1Fresh,
  qm1Open,
  qm10Fresh,
  qm10Open,
}) => Boolean(
  fanStatusFresh
  && anyFanRunning
  && ((qm1Fresh && qm1Open) || (qm10Fresh && qm10Open))
)
