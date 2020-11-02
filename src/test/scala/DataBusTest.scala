import chisel3._
import chisel3.iotesters._
import org.scalatest.{FlatSpec, Matchers}


class DataBusTest(addressSpace: DataBus) extends PeekPokeTester(addressSpace) {

  import Mask._

  val cases = Array(
    (false.B, "h80020000".U(32.W), WORD, "hd8c7b6a5".U(32.W), "0", "0", false),
    (true.B, "h80020000".U(32.W), WORD, "h00000000".U(32.W), "hd8c7b6a5", "0", false),

    (false.B, "h10012000".U(32.W), WORD, "hd8c7b6a5".U(32.W), "0", "0", false),
    (true.B, "h80020000".U(32.W), WORD, "h00000000".U(32.W), "hd8c7b6a5", "hd8c7b6a5", false),
    (true.B, "h0000bff8".U(32.W), WORD, "h00000000".U(32.W), "h00000005", "0", false),
    (false.B, "h00004000".U(32.W), WORD, "h0000000A".U(32.W), "0", "0", false),
    (false.B, "h00004004".U(32.W), WORD, "h00000000".U(32.W), "0", "0", false),
    (true.B, "h0000bff8".U(32.W), WORD, "h00000000".U(32.W), "h00000008", "0", false),
    (true.B, "h0000bff8".U(32.W), WORD, "h00000000".U(32.W), "h00000009", "0", false),
    (true.B, "h0000bff8".U(32.W), WORD, "h00000000".U(32.W), "h0000000A", "0", true),
    (true.B, "h0000bff8".U(32.W), WORD, "h00000000".U(32.W), "h0000000B", "0", true),
  )
  for ((enable_read, address, maskLevel, dataIn, expectedDataOut, expectedGPIOOut, timerIntPending) <- cases) {
    poke(addressSpace.io.read_mode, enable_read)
    poke(addressSpace.io.addr, address)
    poke(addressSpace.io.maskLevel, maskLevel)
    poke(addressSpace.io.dataIn, dataIn)

    step(1)

    if (expectedDataOut != "0") {
      expect(addressSpace.io.dataOut, expectedDataOut.U)
    }
    if (expectedGPIOOut != "0") {
      expect(addressSpace.io.gpioOut, expectedGPIOOut.U)
    }
    expect(addressSpace.io.timerInterruptPending, timerIntPending.B)
  }
}

class DataBusSpec extends FlatSpec with Matchers {
  behavior of "AddressedIOSpec"


  it should "read and write successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new DataBus) { sram =>
      new DataBusTest(sram)
    } should be(true)
  }
}

