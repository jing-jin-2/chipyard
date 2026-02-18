package chipyard.fpga.vcu118

import chisel3._
import chisel3.experimental.{BaseModule}
import org.chipsalliance.cde.config.Parameters

import org.chipsalliance.diplomacy.nodes.{HeterogeneousBag}
import freechips.rocketchip.tilelink.{TLBundle}

import sifive.blocks.devices.uart.{UARTPortIO}
import sifive.blocks.devices.spi.{HasPeripherySPI, SPIPortIO}

import chipyard._
import chipyard.harness._
import chipyard.iobinders._

import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._

import testchipip.util.{ClockedIO}

import freechips.rocketchip.amba.axi4.{AXI4Bundle, AXI4SlaveNode, AXI4MasterNode, AXI4EdgeParameters}

/*** UART ***/
class WithUART extends HarnessBinder({
  case (th: VCU118FPGATestHarnessImp, port: UARTPort, chipId: Int) => {
    th.vcu118Outer.io_uart_bb.bundle <> port.io
  }
})

/*** SPI ***/
class WithSPISDCard extends HarnessBinder({
  case (th: VCU118FPGATestHarnessImp, port: SPIPort, chipId: Int) => {
    th.vcu118Outer.io_spi_bb.bundle <> port.io
  }
})

/*** Experimental DDR ***/
class WithDDRMem extends HarnessBinder({
  case (th: VCU118FPGATestHarnessImp, port: TLMemPort, chipId: Int) => {
    val bundles = th.vcu118Outer.ddrClient.out.map(_._1)
    val ddrClientBundle = Wire(new HeterogeneousBag(bundles.map(_.cloneType)))
    bundles.zip(ddrClientBundle).foreach { case (bundle, io) => bundle <> io }
    ddrClientBundle <> port.io
  }
})

/*** AXI4 MMIO Slave ***/
class WithMMIOSlave extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: AXI4InPort, chipId: Int) => th match {
    case vcu118th: VCU118FPGATestHarnessImp => {
      val axi = vcu118th.vcu118Outer.axi_mmio_slave_bb.bundle
      axi.clock := port.io.clock
      axi.bits.elements("0") <> port.io.bits
    }
    case _ =>
  }
})


class WithJTAG extends HarnessBinder({
  case (th: VCU118FPGATestHarnessImp, port: JTAGPort, chipId: Int) => {
    val jtag_io = th.vcu118Outer.jtagPlacedOverlay.overlayOutput.jtag.getWrappedValue
    port.io.TCK := jtag_io.TCK
    port.io.TMS := jtag_io.TMS
    port.io.TDI := jtag_io.TDI
    jtag_io.TDO.data := port.io.TDO
    jtag_io.TDO.driven := true.B
    // ignore srst_n
    jtag_io.srst_n := DontCare

  }
})
