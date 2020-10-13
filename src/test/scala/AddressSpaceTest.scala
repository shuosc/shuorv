import chisel3._
import chisel3.iotesters._
import org.scalatest.{FlatSpec, Matchers}


class AddressIOTest(addressSpace: AddressSpace) extends PeekPokeTester(addressSpace) {

  import Mask._

  val cases = Array(
    (false.B, 0.U(32.W), WORD, "hd8c7b6a5".U(32.W), "0", "0"),
    (true.B, 0.U(32.W), WORD, "h00000000".U(32.W), "hd8c7b6a5", "0"),

    (false.B, "h40000000".U(32.W), WORD, "hd8c7b6a5".U(32.W), "0", "0"),
    (true.B, 0.U(32.W), WORD, "h00000000".U(32.W), "hd8c7b6a5", "hd8c7b6a5"),
  )
  for ((enable_read, address, maskLevel, dataIn, expectedDataOut, expectedGPIOOut) <- cases) {
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
  }
}

class AddressedIOSpec extends FlatSpec with Matchers {
  behavior of "AddressedIOSpec"


  it should "read and write successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new AddressSpace) { sram =>
      new AddressIOTest(sram)
    } should be(true)
  }
}

