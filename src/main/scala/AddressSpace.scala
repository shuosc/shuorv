import chisel3._
import chisel3.util._

// This is an abstraction among
// interfaces of controllers which can accessed
// like a memory, eg. SRAM, CSR and IO
class AddressIOInterface extends Bundle {
  val read_mode = Input(Bool())
  val addr = Input(UInt(32.W))
  val maskLevel = Input(UInt(2.W))
  val dataIn = Input(UInt(32.W))
  val dataOut = Output(UInt(32.W))
}

class AddressSpace extends Module {
  val io = IO(new AddressIOInterface {
    val gpioOut = Output(UInt(32.W))
  })
  val sram = Module(new ByteAddressedSRAM)
  val gpio = Module(new GPIOController)
  // todo: consider make all submodules only take the low 30 bits, then we can use `<>` here
  sram.io.read_mode := true.B
  sram.io.addr := 0.U(32.W)
  sram.io.dataIn := 0.U(32.W)
  sram.io.maskLevel := Mask.NONE

  gpio.io.read_mode := true.B
  gpio.io.addr := 0.U(32.W)
  gpio.io.dataIn := 0.U(32.W)
  gpio.io.maskLevel := Mask.NONE

  io.dataOut := 0xdead.U(32.W)
  io.gpioOut := gpio.io.dataOut

  switch(io.addr(31, 30)) {
    is("b00".U) {
      sram.io.dataIn := io.dataIn
      sram.io.addr := io.addr
      sram.io.maskLevel := io.maskLevel
      sram.io.read_mode := io.read_mode
      io.dataOut := sram.io.dataOut
    }
    is("b01".U) {
      gpio.io.dataIn := io.dataIn
      gpio.io.addr := Cat("b00".U, io.addr(29, 0))
      gpio.io.maskLevel := io.maskLevel
      gpio.io.read_mode := io.read_mode
      io.dataOut := gpio.io.dataOut
    }
    // others are reserved for CSRs, etc
  }
}
