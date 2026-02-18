package chipyard.fpga.vcu118

import org.chipsalliance.cde.config.Config

class GemminiVCU118Config extends Config(
  new WithFPGAFrequency(25) ++
  new WithVCU118Tweaks ++
  new chipyard.GemminiRocketConfig
)

class GemminiResNetVCU118Config extends Config(
  new WithFPGAFrequency(25) ++
  new WithHackedVCU118Tweaks ++
  new chipyard.GemminiRocketConfig
)

class GemminiUartBootromConfig extends Config(
  new WithFPGAFrequency(25) ++
  new WithUartBootromVCU118Tweaks ++
  new chipyard.GemminiRocketConfig
)

class GemminiUartBootromNvdlaConfig extends Config(
  new WithFPGAFrequency(75) ++
  new WithUartBootromVCU118Tweaks ++
  new chipyard.NVDLALikeGemminiRocketConfig
)