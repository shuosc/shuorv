import glob
import os
from pathlib import Path
import shutil
import sys

shutil.copyfile("./src/main/scala/ProgramROM.scala",
                "./src/main/scala/ProgramROM.scala.backup")
shutil.copyfile("./src/test/scala/TopTest.scala", "./src/test/scala/TopTest.scala.backup")
fail = False
for filenamestr in glob.glob('test_cases/*'):
    filename = Path(filenamestr)
    for asmstr in glob.glob(filenamestr + '/*.asm'):
        asm = Path(asmstr)
        os.system(
            "docker run -i shuosc/shuasm < {} > {}"
            .format(asmstr, asm.parent.joinpath("{}.thex".format(asm.stem)))
        )
    rom = os.popen(
        "python3 ./scripts/integration_test/rom.py ./{}/layout.lot"
        .format(filename)
    ).read()
    expected = os.popen(
        "python3 ./scripts/integration_test/checker.py ./{}/expected.exp"
        .format(filename)
    ).read()
    with open('./src/main/scala/ProgramROM.scala', 'w') as f:
        f.write(rom)
    with open('./src/test/scala/TopTest.scala', 'w') as f:
        f.write(expected)
    return_value = os.system('sbt "testOnly TopSpec" > /dev/null')
    if return_value & 0xff00 != 0:
        print("Test failed: {}".format(filename))
        fail = True
        break

shutil.copyfile("./src/main/scala/ProgramROM.scala.backup",
                "./src/main/scala/ProgramROM.scala")
shutil.copyfile("./src/test/scala/TopTest.scala.backup",
                "./src/test/scala/TopTest.scala")
os.remove("./src/main/scala/ProgramROM.scala.backup")
os.remove("./src/test/scala/TopTest.scala.backup")
if fail:
    sys.exit(1)