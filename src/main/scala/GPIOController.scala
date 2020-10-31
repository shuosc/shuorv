import chisel3._

// A GPIO Controller
// Currently this is for test only,
// a 32-bit register is directly mapped to 32 read-only ports
// todo: consider a better way of handling GPIO
class GPIOController extends Module {
  val io = IO(new DataBusBundle)
  val current_value = RegInit(0.U(32.W))
  io.dataOut := current_value
  when(io.maskLevel =/= Mask.NONE) {
    current_value := io.dataIn
  }
}
