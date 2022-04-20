import chisel3._
import chisel3.iotesters._
import org.scalatest.{FlatSpec, Matchers}

class SerialRecieveControllerTest(serialRecieveController: SerialRecieveController) extends PeekPokeTester(serialRecieveController) {
    poke(serialRecieveController.io.rxWire, true)
    poke(serialRecieveController.io.readMode, true)
    poke(serialRecieveController.io.address, Registers.STATUS)
    poke(serialRecieveController.io.maskLevel, Mask.NONE)
    step(1)

    def send(data: Int): Unit = {
      poke(serialRecieveController.io.rxWire, false)
      step(38400 / 9600)

      for (bitIndex <- 0 to 7) {
        poke(serialRecieveController.io.rxWire, (data & (1 << bitIndex)) != 0)
        step(38400 / 9600)
      }
      poke(serialRecieveController.io.rxWire, true)
      step(38400 / 9600)
    }

    send(0x55)
    send(0xAA)
    send(0xF0)
    send(0x0F)

    poke(serialRecieveController.io.readMode, true)
    poke(serialRecieveController.io.address, Registers.DATA)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, 0x55.U)
    step(1)
    poke(serialRecieveController.io.address, Registers.STATUS)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, "b00000000_00000000_000000_0000_01_00".U)
    poke(serialRecieveController.io.maskLevel, Mask.NONE)
    step(1)
    send(0xbb)
    
    poke(serialRecieveController.io.address, Registers.DATA)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, 0xAA.U)
    step(1)
    step(1)
    expect(serialRecieveController.io.dataOut, 0xF0.U)
    step(1)
    poke(serialRecieveController.io.address, Registers.STATUS)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, "b00000000_00000000_000000_0000_11_01".U)
    step(1)
    poke(serialRecieveController.io.address, Registers.DATA)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, 0x0F.U)
    step(1)
    poke(serialRecieveController.io.address, Registers.STATUS)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, "b00000000_00000000_000000_0000_00_01".U)
    step(1)
    poke(serialRecieveController.io.address, Registers.DATA)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, 0xbb.U)
    step(1)
    poke(serialRecieveController.io.address, Registers.STATUS)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, "b00000000_00000000_000000_0000_01_01".U)
}

class SerialRecieveControllerSpec extends FlatSpec with Matchers {
  behavior of "SerialRecieveControllerSpec"

  it should "read and write successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new SerialRecieveController(38400, 9600, 2)) { serialRecieveController =>
      new SerialRecieveControllerTest(serialRecieveController)
    } should be(true)
  }
}