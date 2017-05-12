package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class MOVEP implements GenInstructionHandler {

	final Gen68 cpu;
	
	public MOVEP(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	MOVEP -- Move peripheral data
//
//SYNOPSIS
//	MOVEP	Dx,(d,Ay)
//	MOVEP	(d,Ay),Dx
//
//	Size = (Word, Long)
//
//FUNCTION
//	Data is transfered between a data register and ever-other
//	byte of memory at the selected address.
//	Transfer is made between a data register and alterned bytes of memory
//	at the selected address, must be specified in indirect mode to An with
//	a 16 bits displacement.
//	This instruction is of use with 8 bits peripheral programing.
//
//	Example:
//	~~~~~~~
//		LEA	port0,A0	; A0 -> $FFFFFFFFFFFFFFFF
//		MOVEQ	#0,D0
//		MOVEP.L	D0,(0,A0)	; A0 -> $FF00FF00FF00FF00
//		MOVE.L	#$55554444,D0
//		MOVEP.L	D0,(1,A0)	; A0 -> $FF55FF55FF44FF44
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|-----------|---|---|---|-----------|
//	| 0 | 0 | 0 | 0 |Dx REGISTER| OP-MODE   | 0 | 0 | 1 |Ay REGISTER|
//	|---------------------------------------------------------------|
//	|                        16 BITS OFFSET                         |
//	-----------------------------------------------------------------
//
//OP-MODE
//	100->16 bits move, memory to register
//	101->32 bits move, memory to register
//	110->16 bits move, register to memory
//	111->32 bits move, register to memory
//
//REGISTER
//	Dx register specifies the number of data register.
//	Ay register specifies the number of address register which takes place
//	in indirect addressing with displacement.
//
//RESULT
//	None.

	@Override
	public void generate() {
		int base = 0x0108;
		GenInstruction ins = null;

		for (int opMode = 0; opMode < 4; opMode++) {
			if (opMode == 0b00) {
				
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						MOVEPMemToRegWord(opcode);
					}
				};
			} else if (opMode == 0b01) {
				
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						MOVEPMemToRegLong(opcode);
					}
				};
			} else if (opMode == 0b10) {
				
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						MOVEPRegToMemWord(opcode);
					}
				};
			} else if (opMode == 0b11) {
				
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						MOVEPRegToMemLong(opcode);
					}
				};
			}
			
			for (int dataReg = 0; dataReg < 8; dataReg++) {
				for (int addrReg = 0; addrReg < 8; addrReg++) {
					int opcode = base | (dataReg << 9) | (opMode << 6) | addrReg;
					cpu.addInstruction(opcode, ins);
				}
			}
		}
	}
	
	private void MOVEPMemToRegWord(int opcode) {
		throw new RuntimeException();
	}

	private void MOVEPMemToRegLong(int opcode) {
		throw new RuntimeException();
	}
	
	private void MOVEPRegToMemWord(int opcode) {
		int addrReg = opcode & 0x7;
		int dataReg = (opcode >> 9) & 0x7;
		
		long offset  = cpu.bus.read(cpu.PC + 2) << 8;
			 offset |= cpu.bus.read(cpu.PC + 3);
		
		cpu.PC += 2;

		long data = cpu.getD(dataReg) & 0xFFFF;
		long addr = cpu.getA(addrReg);
		addr += offset;
		
		cpu.bus.write(addr, data >> 8, Size.BYTE);
		cpu.bus.write(addr + 2, data & 0xFF, Size.BYTE);
	}
	
	private void MOVEPRegToMemLong(int opcode) {
		int addrReg = opcode & 0x7;
		int dataReg = (opcode >> 9) & 0x7;
		
		long offset  = cpu.bus.read(cpu.PC + 2) << 8;
			 offset |= cpu.bus.read(cpu.PC + 3);
		
		cpu.PC += 2;

		long data = cpu.getD(dataReg);
		long addr = cpu.getA(addrReg);
		addr += offset;
		
		cpu.bus.write(addr, (data >> 24), Size.BYTE);
		cpu.bus.write(addr + 2, (data >> 16) & 0xFF, Size.BYTE);
		cpu.bus.write(addr + 4, (data >> 8) & 0xFF, Size.BYTE);
		cpu.bus.write(addr + 6, data & 0xFF, Size.BYTE);
	}
	
}
