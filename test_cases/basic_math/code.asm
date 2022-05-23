main:
  li a0, 0x10012000
  sw zero, 0(a0)
  li a2, 1
  li a3, 2
  add a2, a2, a3
  sw a2, 0(a0)
  li a2, 3
  li a3, 2
  sub a2, a2, a3
  sw a2, 0(a0)
  sub a2, a2, a3
  sw a2, 0(a0)
  sll a2, a2, a3
  sw a2, 0(a0)
  sra a2, a2, a3
  addi a2, a2, 1
  sw a2, 0(a0)
  addi a2, a2, -2
  sw a2, 0(a0)
  slli a2, a2, 3
  sw a2, 0(a0)
  srai a2, a2, 2
  sw a2, 0(a0)
