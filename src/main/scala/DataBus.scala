import chisel3._

// This is an abstraction among
// interfaces of controllers which can accessed
// like a memory, eg. SRAM, CSR and IO
class DataBusBundle extends Bundle {
  val readMode = Input(Bool())
  val address = Input(UInt(32.W))
  val maskLevel = Input(UInt(2.W))
  val dataIn = Input(UInt(32.W))
  val dataOut = Output(UInt(32.W))
}

class DataBus extends Module {
  val DATA_BASE_ADDRESS = "h80020000"
  val DATA_END_ADDRESS = "h90000000"
  val GPIO_BASE_ADDRESS = "h10012000"
  val GPIO_END_ADDRESS = "h10013000"
  val SERIAL_BASE_ADDRESS = "h10014000"
  val SERIAL_END_ADDRESS = "h10015000"

  val io = IO(new DataBusBundle {
    val timerInterruptPending = Output(Bool())
    val gpioOut = Output(UInt(32.W))
    val serialRx = Input(Bool())
    val serialTx = Output(Bool())
  })

  val timer = Module(new Timer)
  val sram = Module(new ByteAddressedSRAM)
  val gpio = Module(new GPIOController)
  val serial = Module(new SerialController(25000000,9600,3))

  timer.io.readMode := true.B
  timer.io.address := 0.U(32.W)
  timer.io.dataIn := 0.U(32.W)
  timer.io.maskLevel := Mask.NONE

  sram.io.readMode := true.B
  sram.io.address := 0.U(32.W)
  sram.io.dataIn := 0.U(32.W)
  sram.io.maskLevel := Mask.NONE

  gpio.io.readMode := true.B
  gpio.io.address := 0.U(32.W)
  gpio.io.dataIn := 0.U(32.W)
  gpio.io.maskLevel := Mask.NONE

  serial.io.readMode := true.B
  serial.io.address := 0.U(32.W)
  serial.io.dataIn := 0.U(32.W)
  serial.io.maskLevel := Mask.NONE

  io.dataOut := 0xdead.U(32.W)
  io.gpioOut := gpio.io.dataOut
  
  io.timerInterruptPending := timer.io.interruptPending
  io.serialTx := serial.io.txWire
  serial.io.rxWire := io.serialRx

  when(DATA_BASE_ADDRESS.U <= io.address & io.address < DATA_END_ADDRESS.U) {
    sram.io.dataIn := io.dataIn
    sram.io.address := io.address - DATA_BASE_ADDRESS.U
    sram.io.maskLevel := io.maskLevel
    sram.io.readMode := io.readMode
    io.dataOut := sram.io.dataOut
  }.elsewhen(GPIO_BASE_ADDRESS.U <= io.address & io.address < GPIO_END_ADDRESS.U) {
    gpio.io.dataIn := io.dataIn
    gpio.io.address := io.address - GPIO_BASE_ADDRESS.U
    gpio.io.maskLevel := io.maskLevel
    gpio.io.readMode := io.readMode
    io.dataOut := gpio.io.dataOut
  }.elsewhen(SERIAL_BASE_ADDRESS.U <= io.address & io.address < SERIAL_END_ADDRESS.U) {
    serial.io.dataIn := io.dataIn
    serial.io.address := io.address - SERIAL_BASE_ADDRESS.U
    serial.io.maskLevel := io.maskLevel
    serial.io.readMode := io.readMode
    io.dataOut := serial.io.dataOut
  }.otherwise {
    timer.io.dataIn := io.dataIn
    timer.io.address := io.address
    timer.io.maskLevel := io.maskLevel
    timer.io.readMode := io.readMode
    io.dataOut := timer.io.dataOut
  }
}
