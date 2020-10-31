import chisel3._

// This is an abstraction among
// interfaces of controllers which can accessed
// like a memory, eg. SRAM, CSR and IO
class DataBusBundle extends Bundle {
  val read_mode = Input(Bool())
  val addr = Input(UInt(32.W))
  val maskLevel = Input(UInt(2.W))
  val dataIn = Input(UInt(32.W))
  val dataOut = Output(UInt(32.W))
}

class DataBus extends Module {
  val DATA_BASE_ADDRESS = "h80020000"
  val DATA_END_ADDRESS = "h90000000"
  val GPIO_BASE_ADDRESS = "h10012000"
  val GPIO_END_ADDRESS = "h10013000"

  val io = IO(new DataBusBundle {
    val gpioOut = Output(UInt(32.W))
  })
  val sram = Module(new ByteAddressedSRAM)
  val gpio = Module(new GPIOController)
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

  when(DATA_BASE_ADDRESS.U <= io.addr & io.addr < DATA_END_ADDRESS.U) {
    sram.io.dataIn := io.dataIn
    sram.io.addr := io.addr - DATA_BASE_ADDRESS.U
    sram.io.maskLevel := io.maskLevel
    sram.io.read_mode := io.read_mode
    io.dataOut := sram.io.dataOut
  }.elsewhen(GPIO_BASE_ADDRESS.U <= io.addr & io.addr < GPIO_END_ADDRESS.U) {
    gpio.io.dataIn := io.dataIn
    gpio.io.addr := io.addr - GPIO_BASE_ADDRESS.U
    gpio.io.maskLevel := io.maskLevel
    gpio.io.read_mode := io.read_mode
    io.dataOut := gpio.io.dataOut
  }
}
