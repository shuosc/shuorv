import os
import shutil


shutil.copyfile("./src/main/scala/ProgramROM.scala.backup",
                "./src/main/scala/ProgramROM.scala")
shutil.copyfile("./src/test/scala/TopTest.scala.backup",
                "./src/test/scala/TopTest.scala")
os.remove("./src/main/scala/ProgramROM.scala.backup")
os.remove("./src/test/scala/TopTest.scala.backup")
