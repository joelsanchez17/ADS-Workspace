Surfer Waveform Notes
=====================

Surfer displays the generated Chisel/FIRRTL hierarchy, so some temporary
signals may have compiler-generated or unusual names.

For Task 3 / Level 1 debugging, prefer named module paths and stable signals:
IF, ID, EX, MEM, WB barriers, register file, PC, instruction, writeback result,
and the top-level debug outputs under `io.dbg`.
